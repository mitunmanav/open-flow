# 0002: Dictation state machine

Date: 2026-10-02

## Status

Accepted

## Context

The dictation lifecycle fans out across bubble, audio, provider, refiner, and inserter. Without one explicit state machine, state lives as scattered booleans (`isRecording`, `isProcessing`, …) which admit impossible combinations.

## Decision

One `DictationController` owns a `StateFlow<DictationState>`; every other component (bubble, audio source, provider, refiner, inserter) sends `DictationEvent`s through one channel. No component writes state directly.

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

The bubble renders purely from `DictationState` payload; history/retry read from terminal outcomes. Testing is a transition-table test plus FakeProvider-driven pipeline tests.
