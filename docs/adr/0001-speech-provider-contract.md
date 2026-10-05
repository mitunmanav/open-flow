# 0001: SpeechProvider contract shape

Date: 2026-10-02

## Status

Accepted — amended 2026-10-02 (`.scratch/openflow-v1/issues/22-providerhealth-cost-shape.md`): `health()` gains a declared return type and `ProviderHealth` state set; `estimatedCostAvailability` replaced by a nullable declared rate; the meaning of `recoverable` is now stated.

Amended 2026-10-03 (`.scratch/openflow-v1/issues/31-latency-guard.md`): `DEGRADED` gains slowness as one of its causes, narrowed by the router from measured Final Latency. **This contract gains no new member to detect a slow device.**

## Context

OpenFlow must not hard-wire to one STT engine. The bubble, audio, history, refinement, and insertion layers all depend on whatever the speech engine does, so the boundary between them had to be designed before any engine work.

Alternatives considered:

- Callback/listener interfaces (typical of native Android speech APIs): harder to compose, cancellation is manual.
- Direct calls into provider SDKs from the bubble/controller: total lock-in, untestable.

## Decision

- `SpeechProvider` exposes `prepare()`, `transcribe(request)`, `health(): ProviderHealth`, `close()`, and a `StateFlow<ProviderState>` (`NOT_PREPARED/PREPARING/READY/CLOSED`) — the only reactive surface in V1.
- `ProviderHealth` is a flat enum, a separate type from `ProviderState` that never shares its value set: `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, `MODEL_MISSING`. `READY` (lifecycle) and `HEALTHY` (readiness) are different concepts and no health value mirrors a lifecycle value. Invariant: `ProviderState.READY` implies health ∈ {`HEALTHY`, `DEGRADED`, `UNAVAILABLE`}, so `MODEL_MISSING` is necessarily `NOT_PREPARED`. `DEGRADED` is usable-but-worse, `UNAVAILABLE` is transient, `MODEL_MISSING` needs a download rather than a retry — three distinct remediations, so three distinct values.
- `health()` is a plain synchronous method reading the adapter's cached local state: no active probe, no network call, no `StateFlow`. Only the adapter authors it (model on disk, engine loaded, config present, last failure); the router may narrow a verdict by exclusion but never upgrades one, which is what keeps `choose()` a pure function of its inputs. V1's only consumer is the router reading a snapshot at `PREPARING`; a health stream would invite the automatic health-based rerouting ADR-0004 defers.
- **Detecting a device too slow for its model adds nothing to this contract.** The router derives Final Latency from timings it already receives — `Final`'s `endedAtMs` against the last audio timestamp — and *narrows* an existing verdict to `DEGRADED`; it never adds one. Slowness joins model-on-disk, engine-loaded and last-failure as a cause of `DEGRADED`, which stays a flat enum: the cause is recorded in the router's decision log, where "which rule excluded this provider" already lives, not in the type. No `SpeechEvent` member carries latency and no capability declares an expected lag — a capability is a static claim about a model, slowness is an observation about *this* device over time, and conflating them would make an undeclared capability indistinguishable from a measured fault. The live signal belongs on a health stream, which is exactly what ADR-0001 declines to add.
- `transcribe(request)` returns a cold `Flow<SpeechEvent>`; cancellation of the coroutine cancels transcription.
- `SpeechEvent` is sealed: `Preparing`, `Listening`, `Partial(text)`, `Final(text, startedAtMs, endedAtMs)`, `Failure(reason, recoverable)`.
- `Partial` snapshots are cumulative per utterance; `Partial` events are conflated downstream (keep latest); `Preparing`/`Final`/`Failure` are never dropped.
- `FailureReason` is a typed enum (`OfflineModelMissing`, `UnsupportedLanguage`, `AudioCaptureFailed`, `Timeout`, `Busy`, `Cancelled`, `Unknown`) because the router keys fallback off it. `recoverable` means *the dictation can still complete by another route*, not *this call will succeed*, so `OfflineModelMissing` is recoverable — ADR-0004 falls back to the next eligible provider and the "download the speech model" prompt belongs on the user's screen only once every candidate has failed. Invariant, enforced by Contract Tests: a provider reporting `MODEL_MISSING` from `health()` must fail `transcribe()` with `OfflineModelMissing`, never something generic.
- Capabilities drive all app behavior — no `if provider == X` anywhere: `streaming`, `offline`, `supportedLanguages`, `autoDetectLanguage`, `partialTranscripts`, `timestamps`, `confidence`, `vocabularyBias`, `pricing`, `maxAudioDurationSeconds`.
- `pricing: Pricing?` carries `microsUsdPerSecond: Long` and replaces the old `estimatedCostAvailability` boolean — nullness already says "cannot estimate", so keeping both would recreate the two-sources-of-truth defect nullability exists to remove. `null` means *cannot estimate*; `0` means *known free*, and local providers declare `0`. Amounts are integer micros of USD, USD-only in V1: a provider billing in another currency converts at its own boundary or declares `pricing = null`. The router derives the worst case from the declared rate and `maxAudioDurationSeconds` rather than trusting an adapter-supplied number, because a cost ceiling that an adapter can overstate is not a ceiling.
- One provider instance per model configuration; language is per-request. Swapping models = new instance held by the router.
- Endpointing (when the user stopped talking) is owned by the provider emitting `Final`; the controller applies a max-duration guard from capabilities. Timestamps are utterance-level in V1.
- `FakeProvider` (scripted events, latency, injected failures) runs the whole pipeline in tests.

## Consequences

Future providers are pure adapters. The router, history, and refiner treat every engine identically, and provider-contract tests can exercise the full pipeline without a model on disk.

Health and lifecycle are now two vocabularies rather than one, which is the point: a future contributor reading `ProviderState.READY` beside `ProviderHealth.HEALTHY` will be tempted to merge the two types, and they must not merge — `CLOSED` is something the app did, not something the engine reported. Likewise `pricing = null` will look like a missing value worth defaulting to `0`; it is not, it means the provider cannot estimate, and defaulting it to zero would make a paid API look free and pass every ceiling check.
