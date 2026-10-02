# Define the SpeechProvider contract

Type: grilling
Status: resolved
Blocked by: 01

## Question

What is the exact V1 `SpeechProvider` interface: identity, `capabilities` fields, `prepare()`, `transcribe(request)` event stream (Preparing/Listening/Partial/Final/Failure), `health()`, `close()`, plus the FakeProvider spec used to test the whole pipeline without a real engine? Produce the interface spec for `core/stt`.

## Answer

Contract settled (full detail in `docs/adr/0001-speech-provider-contract.md`, vocabulary in `GLOSSARY.md`):

- `SpeechProvider`: `prepare()`, `transcribe(request): Flow<SpeechEvent>`, `health()`, `close()`, plus `StateFlow<ProviderState>`.
- `TranscribeRequest`: audio config (16 kHz, 16-bit PCM mono), `languageHint`, `autoDetectLanguage`, `enablePartialResults`, `vocabularyBias`, `AudioSource` flow from `core/audio`.
- `SpeechEvent`: `Preparing`, `Listening`, `Partial(text)` (cumulative, conflated), `Final(text, startedAtMs, endedAtMs)`, `Failure(FailureReason, recoverable)` with typed reasons (`OfflineModelMissing`, `UnsupportedLanguage`, `AudioCaptureFailed`, `Timeout`, `Busy`, `Cancelled`, `Unknown`).
- Capabilities: streaming, offline, supportedLanguages, autoDetectLanguage, partialTranscripts, timestamps, confidence, vocabularyBias, estimatedCostAvailability, maxAudioDurationSeconds. No `if provider == X`.
- `health()`: `READY | NOT_READY(reason) | DEGRADED | UNAVAILABLE`, cheap local check.
- One instance per model configuration; language per request. Endpointing owned by provider; controller enforces max-duration guard. Utterance-level timestamps only in V1.
- `FakeProvider` is scriptable (event script, per-event latency, injected failure) for end-to-end pipeline tests.
