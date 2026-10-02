# 0001: SpeechProvider contract shape

Date: 2026-10-02

## Status

Accepted

## Context

OpenFlow must not hard-wire to one STT engine. The bubble, audio, history, refinement, and insertion layers all depend on whatever the speech engine does, so the boundary between them had to be designed before any engine work.

Alternatives considered:

- Callback/listener interfaces (typical of native Android speech APIs): harder to compose, cancellation is manual.
- Direct calls into provider SDKs from the bubble/controller: total lock-in, untestable.

## Decision

- `SpeechProvider` exposes `prepare()`, `transcribe(request)`, `health()`, `close()`, and a `StateFlow<ProviderState>` (`NOT_PREPARED/PREPARING/READY/CLOSED`).
- `transcribe(request)` returns a cold `Flow<SpeechEvent>`; cancellation of the coroutine cancels transcription.
- `SpeechEvent` is sealed: `Preparing`, `Listening`, `Partial(text)`, `Final(text, startedAtMs, endedAtMs)`, `Failure(reason, recoverable)`.
- `Partial` snapshots are cumulative per utterance; `Partial` events are conflated downstream (keep latest); `Preparing`/`Final`/`Failure` are never dropped.
- `FailureReason` is a typed enum (`OfflineModelMissing`, `UnsupportedLanguage`, `AudioCaptureFailed`, `Timeout`, `Busy`, `Cancelled`, `Unknown`) because the router keys fallback off it.
- Capabilities drive all app behavior — no `if provider == X` anywhere: `streaming`, `offline`, `supportedLanguages`, `autoDetectLanguage`, `partialTranscripts`, `timestamps`, `confidence`, `vocabularyBias`, `estimatedCostAvailability`, `maxAudioDurationSeconds`.
- One provider instance per model configuration; language is per-request. Swapping models = new instance held by the router.
- Endpointing (when the user stopped talking) is owned by the provider emitting `Final`; the controller applies a max-duration guard from capabilities. Timestamps are utterance-level in V1.
- `FakeProvider` (scripted events, latency, injected failures) runs the whole pipeline in tests.

## Consequences

Future providers are pure adapters. The router, history, and refiner treat every engine identically, and provider-contract tests can exercise the full pipeline without a model on disk.
