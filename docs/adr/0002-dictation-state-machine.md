# 0002: Dictation state machine

Date: 2026-10-02

## Status

Accepted — amended 2026-10-04: controller-owned target and settings snapshots; see [Where does app identity enter the pipeline?](../../.scratch/openflow-v1/issues/52-where-does-app-identity-enter.md).

Amended 2026-10-05: idle Bubble interaction for explicit History placement; see
[How does History retry choose a new destination?](../../.scratch/openflow-v1/issues/61-history-retry-destination.md).

## Context

The dictation lifecycle fans out across bubble, audio, provider, refiner, and inserter. Without one explicit state machine, state lives as scattered booleans (`isRecording`, `isProcessing`, …) which admit impossible combinations.

## Decision

One `DictationController` owns a `StateFlow<DictationState>`; every other component (bubble, audio source, provider, refiner, inserter) sends `DictationEvent`s through one channel. No component writes state directly.

Before recording begins, the controller captures and owns an immutable Target Snapshot
(package plus the insertion characteristics in ADR-0003) from the same target observation
used for the sensitive-field check. No writable target or no identifiable package refuses
start with the microphone closed and a prompt to focus a writable field.

The controller freezes the applicable dictionary, snippet and style settings at the start.
The refiner receives the original package name and those frozen settings; the inserter
receives the original target information. Both snapshots remain owned by the controller
across state transitions. A change of active app or settings does not change this Dictation.
RouterContext and speech-provider requests need no app identity.

States (payload-carrying sealed class):

- `Idle`
- `Preparing`
- `Recording(startedAtMs)`
- `Transcribing` — recording stopped, provider finalizing; with streaming providers, partial transcription overlaps `Recording`
- `Refining(rawText)`
- `Inserting(refinedText, targetDescription)`
- `Done(transcriptId, outcome)` — terminal; auto-returns to `Idle` after the outcome is recorded
- `Cancelled` — terminal; auto-returns to `Idle`; audio discarded, nothing persisted
- `ErrorRecoverable(reason, transcriptId?)` — bubble shows problem state; offers retry / copy; transcript preserved if one exists; returns to `Idle`
- `ErrorFatal(reason)` — e.g. mic permission revoked, model unloadable; next start re-enters `Preparing`

Rules:

- Every (state, event) pair has exactly one defined transition or is explicitly ignored; covered by a transition-table unit test.
- `Inserting` is non-cancellable: insertion either lands or fails; the bubble reflects the outcome.
- Cancel during `Recording`/`Transcribing`/`Refining`: stop capture, cancel provider flow, discard audio, go `Cancelled`. No transcript is persisted.
- Every suspending step has a timeout; on timeout → `ErrorRecoverable(Timeout)`.
- Refiner failure degrades to the raw transcript (marked unprocessed) rather than failing the dictation — losing the user's words to a cleanup bug is the worst outcome.

## Consequences

During a Dictation the Bubble renders purely from `DictationState` payload; History reads
terminal outcomes. Between Dictations, the idle Bubble may instead present the pending
History placement flow in ADR-0003. That flow uses a fresh selected destination and stored
text, without recording, transcription or refinement; cancellation, success or process
interruption clears its pending authority. It introduces no additional Bubble lifecycle
state or microphone service. Testing is a transition-table test plus FakeProvider-driven
pipeline tests.
