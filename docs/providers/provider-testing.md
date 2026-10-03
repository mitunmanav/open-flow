# Testing a SpeechProvider

`Capabilities Honesty` is the rule that makes this codebase's provider seam
trustworthy, and the Contract Test suite is what makes that rule enforceable
rather than aspirational.

Read [`GLOSSARY.md`](../../GLOSSARY.md) first — `Capabilities Honesty`,
`Contract Test`, `Provider State`, `Provider Health`, and `Declared Rate` all
carry meanings narrower than their everyday senses.

## Why a shared suite

A provider's capabilities are declarations the rest of the app believes without
re-checking. The router selects on them, the UI builds toggles from them, and the
cost ceiling is computed from them. Nothing else in the codebase verifies that
the engine actually does what it said, so this suite is the only place that can.

Without it, "declared `partialTranscripts`" and "emits `Partial`" are two
independent claims that drift apart silently — usually as a new engine version
that quietly stops streaming. The failure surfaces later, in someone else's
provider, as a UI that offers a live transcript which never updates.

So the suite lives in `core`'s test fixtures and every adapter runs it. It is not
a template you copy and adapt; running a private copy of it proves nothing about
the shared contract.

## Running it

Each provider module runs the shared suite against its adapter. Once the Gradle
skeleton lands, that is a module-scoped test task; `FakeProvider` carries the
same expectations so `core`'s own pipeline tests exercise the contract with no
model on disk.

Attach the output of a passing run to your pull request. A provider proposal
whose tests have not been run is not reviewable, because the interesting
question about a new engine is always "what does it do that we did not ask it
to do", and only a run answers that.

## What the suite asserts

### A declared capability is binding

This is the core of it. For each capability you declare, the suite asserts the
behaviour that makes the declaration true:

- **Declare `partialTranscripts`, emit `Partial`.** The suite feeds audio and
  asserts that a `Partial` arrives before `Final`. An engine that only produces
  final output must declare `partialTranscripts = false`, which is a correct
  answer the router can act on.
- **Declare a language, serve it.** `transcribe` must not fail
  `UnsupportedLanguage` for any code in `supportedLanguages`. This is checked
  across the whole declared set, not a sample, because an engine that handles
  `en-US` and quietly fails `en-GB` is the common real shape of this bug.
- **Declare `timestamps`, mean it.** `startedAtMs` and `endedAtMs` must be
  consistent with the audio actually supplied.
- **Declare `confidence`, produce it.** A declared confidence of `0.0` on every
  utterance is not a confidence score.
- **Declare `offline`, work offline.** The suite runs offline cases with no
  network available. This catches an engine whose "offline" mode still resolves
  a token or a licence check on first use.
- **`maxAudioDurationSeconds` is a real ceiling**, and the controller relies on it
  being one.

`autoDetectLanguage` and `vocabularyBias` are asserted the same way: a
declaration that survives only when the thing it declares is exercised is a
declaration with no evidence behind it.

### Events behave like events

- `Partial` snapshots are **cumulative** — each contains the whole utterance so
  far, not a delta. An adapter that emits diffs looks correct in isolation and
  produces garbled live transcripts once conflated downstream.
- A request produces zero or one terminal event: exactly one `Final` or one
  `Failure`.
- `Failure` carries a specific `FailureReason` wherever one applies, and
  `recoverable` is set the way
  [ADR-0001](../adr/0001-speech-provider-contract.md) defines it: true when the
  dictation can still complete by another route, not when a retry of the same
  call might succeed.
- The flow is **cold** — collecting it twice starts two transcriptions — and
  cancelling the collecting coroutine actually stops the engine. A hot flow or a
  cancellation that only closes a listener leaks native audio and compute.

### Health and lifecycle do not blur

Two vocabularies, and the suite keeps them apart:

| `ProviderState` | Lifecycle — what the app did |
|---|---|
| `NOT_PREPARED` | Created, nothing loaded. |
| `PREPARING` | `prepare()` in flight. |
| `READY` | Loaded and warmed. |
| `CLOSED` | Released. |

| `ProviderHealth` | Readiness — what the engine reports |
|---|---|
| `HEALTHY` | Can serve now. |
| `DEGRADED` | Usable but worse; eligible but ranked last rather than sinking. |
| `UNAVAILABLE` | Transient; a retry may work. |
| `MODEL_MISSING` | Weights absent; needs a download, never a retry. |

`READY` and `HEALTHY` are not two values of one enum, and no health value mirrors
a lifecycle value. `CLOSED` is something the app did, not something an engine
reported, so it has no health counterpart and health has no closed value.

The suite asserts the invariant that makes the split load-bearing: **`READY`
implies health is one of `HEALTHY`, `DEGRADED`, or `UNAVAILABLE`.** A provider
reporting `MODEL_MISSING` while `READY` is a contradiction — it cannot have
finished loading a model it says is missing — and the suite fails it. The
converse is checked too: a provider that reports `MODEL_MISSING` must fail
`transcribe` with `OfflineModelMissing`, never something generic. That pairing
is what lets the router show a download prompt rather than a retry button for a
model that is simply not there.

### `health()` does not probe

`health()` is a plain synchronous method over cached local state — model on disk,
engine loaded, config present, last failure. The suite calls it on an adapter
with no network available and asserts it still answers. V1 does no active probing,
because the only consumer is the router reading one snapshot at `PREPARING`, and
a health check that blocked would sit on the critical path of every dictation.

A health value that needs a round trip to produce it does not belong in `health()`.

### `pricing`: `0` and `null` are different answers

This is the one with a real cost consequence.

| Value | Means |
|---|---|
| `0` | Known free. Local providers declare this. |
| `null` | **Cannot estimate.** |

`null` is not a missing value to be defaulted, and it is emphatically not `0`. An
adapter that reports `null` "because we don't know" is correct and honest; an
adapter that coerces it to `0` makes a metered cloud API look free, and that
adapter then passes every cost-ceiling check while billing real money. There is
no provider in V1 for which that mistake is harmless, which is why V1 ships no
billable provider at all and why the field exists so that the first cloud adapter
is a drop-in rather than a retrofit.

Amounts are integer micros of USD. V1 is USD-only: a provider billing in another
currency converts at its own boundary or declares `pricing = null`. It does not
compute an exchange rate.

The declared rate must be the rate actually charged — that is part of
`Capabilities Honesty`, not a rounding tolerance. Where a rate depends on region
or volume, declare the rate for the configuration you actually ship, and say so
in the proposal.

## FakeProvider

`FakeProvider` is a scripted `SpeechProvider` — scripted events, injected
latency, injected failures — that runs the whole pipeline with no engine and no
model on disk. Use it to test the dictation state machine, the router's decision
log, and the refiner, and use it to reproduce a provider bug as a script before
you chase it through native code.

It carries the same contract expectations as any other adapter. It is not a
mock that may violate the contract because it is a test double — a `FakeProvider`
that emits deltas instead of cumulative snapshots would hide the exact class of
bug this suite exists to catch.

## Checklist before opening a pull request

- Every declared capability has a passing assertion, not a manual check.
- `supportedLanguages` is asserted across the whole declared set.
- `health()` answers with no network, and every value you can return has a stated
  condition behind it.
- `MODEL_MISSING` pairs with a `NOT_PREPARED` state and an `OfflineModelMissing`
  failure.
- `pricing` is `0` only if the provider is genuinely free.
- Cancellation stops the engine.
- The output of a passing run is attached.

See [provider-authoring.md](provider-authoring.md) for the contract itself and
[provider-proposal.md](provider-proposal.md) for what a proposal must answer.