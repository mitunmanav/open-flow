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

`recoverable` means the Dictation can still complete by another route — not that the same call will succeed. A missing model is recoverable because another provider can serve the request.

## Provider State

Lifecycle of a `SpeechProvider` instance: `NOT_PREPARED`, `PREPARING`, `READY`, `CLOSED`. One instance serves one model configuration; language is per request. A `READY` instance is warmed up, not necessarily able to serve — see Provider Health.
_Avoid_: readiness, health, ready

## Provider Health

The readiness verdict `health()` returns about whether a provider can serve a dictation right now, and if not, why: `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, `MODEL_MISSING`. A separate concept from Provider State, which tracks an instance's lifecycle; `READY` and `HEALTHY` are not two values of one enum, and no health value mirrors a lifecycle value. Cheap local truth — V1 does no active network probing.
_Avoid_: ProviderState, health state, ready

## Declared Rate

What a provider charges for audio, declared as micros of USD per second. `null` means it cannot estimate, which is not the same as free — free is `0`, and local providers declare `0`.
_Avoid_: estimated cost availability, pricing flag

## Cost Ceiling

The most a single Dictation may cost, in micros of USD, and no ceiling at all when unset. Compared against the worst case a request could reach, not the cost it turns out to have, so the ceiling holds from the moment a provider is chosen.
_Avoid_: cost limit, budget

## Provider Adapter

A concrete `SpeechProvider` implementation, as distinct from the contract itself. V1 has two, both written by us: FakeProvider for tests and SherpaOnnxProvider for real dictation. Future third-party adapters land as pull requests, not plugins.
_Avoid_: plugin, extension, backend

## Capabilities Honesty

The rule that a declared capability is binding. A provider advertising `partialTranscripts` must emit Partial, a declared language must not fail `UnsupportedLanguage`, and a Declared Rate must be the rate actually charged. Enforced by Contract Tests, because SpeechProvider capabilities drive all app behavior and nothing else verifies them.
_Avoid_: feature detection

## Contract Test

A test that runs a provider's own behaviour against the shared suite in core's test fixtures, asserting its events and capabilities hold together. What makes Capabilities Honesty enforceable rather than aspirational.
_Avoid_: integration test, end-to-end test

## RouterContext

Inputs to one routing decision: `privacyMode` (local-only / cloud-allowed), `offline`, `costCeilingMicrosUsd`, `preferredProviderId`.

## RoutingDecision

The router's output: chosen provider, candidates considered, deciding rule, and the eligible fallback chain — or, when nothing survives, which rule excluded everything and why each candidate went. Recorded on every dictation.

## FakeProvider

A test SpeechProvider implementation used to run the whole dictation pipeline without a real speech engine.

## Acceptance Gate

The protocol that decides whether OpenFlow may claim a release. Fourteen scenarios per device class, judged on whether **the pipeline behaved** — state machine, insertion, router degradation, recovery — and never on transcription accuracy, which belongs to the model. Two bars: *Releasable* (a prerelease tag, one class) and *Shipped* (`v1.0.0`, all three). See `docs/quality/acceptance-gate.md`.
_Avoid_: test suite, QA pass, release checklist

## Device Class

One of **Pixel-like**, **Samsung-class**, or **Xiaomi-class**, decided by behaviour rather than brand: how aggressively the OEM kills background apps, whether an overlay survives, and whether background microphone access is restricted. A Samsung with "never sleeping apps" enabled is still Samsung-class; a Xiaomi-branded phone with aggressive killing turned off may be Pixel-like. Recorded with OEM skin and Android major version.
_Avoid_: device model, phone type, OEM tier

## Recover Cleanly

The post-conditions a Dictation must satisfy after a deliberately injected failure: no stranded Bubble, no orphaned recording on disk, the transcript discarded or preserved exactly as the privacy policy requires, the clipboard holding what it held before, re-insert from History working or the entry cleanly abandonable, no duplicated text in the target field, and a state trace that reached a terminal outcome. A gate cannot judge this without splitting it from *how* the failure is injected.
_Avoid_: handles errors, recovers gracefully, fails safe

## Gate Status

The machine-readable half of the acceptance gate's record: which device classes have been run, on which commit and `version_code`, and how many scenarios are green in each. The **only** thing CI parses. Empty is not a pass — it is an absence of evidence, and it blocks both bars.
_Avoid_: gate results, coverage report, test status
