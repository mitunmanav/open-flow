# Glossary

## Adaptive Dictation Router

The provider-neutral layer that chooses or falls back between STT providers using privacy, language, device capability, connectivity, latency, reliability, and cost signals. In V1 it ships as manual preference + health checks + fallback + logging; automatic ML routing is out of scope.

## Bubble

The floating overlay control (`TYPE_APPLICATION_OVERLAY`) the user taps, holds, drags, or cancels to drive dictation. It is the only UI surface during a dictation session.

## Dictation

One full capture-to-insertion cycle: start a recording, transcribe it, refine the transcript, insert it at the cursor, or recover it. A Dictation has a lifecycle (state machine, see `docs/adr/0002-dictation-state-machine.md`) and exactly one terminal outcome (`Done`, `Cancelled`, `ErrorRecoverable`, `ErrorFatal`).

## Dictation Controller

The component that owns the Dictation state machine and sequence: IDLE → PREPARING → RECORDING → TRANSCRIBING → REFINING → INSERTING → DONE, with CANCELLED / ERROR_RECOVERABLE / ERROR_FATAL from any state.

## SpeechProvider

The stable contract for STT engines. Reports `capabilities` (streaming, offline, languages, timestamps, confidence, cost), `prepare()`, `transcribe(request)` as a stream of normalized events (Preparing, Listening, Partial, Final, Failure), `health()`, `close()`. Implementations include FakeProvider (tests), SherpaOnnxProvider (V1), and future cloud adapters.

## TranscriptRefiner

The provider-independent cleanup stage: deterministic normalization → spoken commands → backtracking → filler removal → list detection → dictionary → snippets → app-style formatting. The advanced-refiner slot is reserved (identity in V1). Failure degrades to raw transcript marked unprocessed. See `docs/architecture/refiner.md`.

## Safe Text Inserter

The component that finds the target editable field again at insertion time, verifies it is still editable and not sensitive, inserts at the selection (SET_TEXT → PASTE → copy+prompt), restores cursor, and falls back to copy/paste recovery. Sensitive fields refuse before recording. See `docs/adr/0003-safe-text-insertion.md`.

## SpeechEvent

A normalized event from a `SpeechProvider` during a transcription: `Preparing`, `Listening`, `Partial(text)` (cumulative snapshot, conflated), `Final(text, startedAtMs, endedAtMs)`, `Failure(reason, recoverable)`.

## ProviderState

Lifecycle of a `SpeechProvider` instance: `NOT_PREPARED`, `PREPARING`, `READY`, `CLOSED`. One instance serves one model configuration; language is per request.

## RouterContext

Inputs to one routing decision: `privacyMode` (local-only / cloud-allowed), `offline`, `costCeiling`, `preferredProviderId`.

## RoutingDecision

The router's output: chosen provider, candidates considered, deciding rule, and the eligible fallback chain. Recorded on every dictation.

## FakeProvider

A test SpeechProvider implementation used to run the whole dictation pipeline without a real speech engine.
