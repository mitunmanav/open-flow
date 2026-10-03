# Authoring a SpeechProvider

How to add a speech-to-text engine to OpenFlow.

Read [`GLOSSARY.md`](../../GLOSSARY.md) first. `SpeechProvider`, `Provider
Adapter`, `Provider State`, `Provider Health`, `Declared Rate`, and
`Capabilities Honesty` all carry precise meanings, and several of them differ
from their everyday senses.

## The contract is internal, not stable

**`SpeechProvider` is an internal contract, not a stable API.** It may change at
any point within the 1.x line. Revisions to
[ADR-0001](../adr/0001-speech-provider-contract.md) and entries in
[`CHANGELOG.md`](../../CHANGELOG.md) are how changes are announced — not
semantic versioning, because the contract has never been versioned
independently.

This is deliberate, from
[ADR-0006](../adr/0006-provider-authoring-no-sdk.md): V1 ships two adapters, both
written by us, and a published SDK before a second real provider exists would be
an empty module plus an API-stability promise we would then have to honour by
refusing to fix our own bugs. Read that sentence before you write against this
contract. Without it, you will reasonably assume the shape below is fixed, build
around it, and be wrong at 1.3.

There is no plugin system and no external write access. A provider arrives as a
pull request.

## What a provider is

A provider is one Gradle module under `providers/<name>/` that depends on
`core` and nothing else, plus one registry entry in `app/`. It must not import
`app`, another provider, or anything outside `core`'s public contract types —
that dependency rule is the whole reason the router, refiner, and insertion
layers never learn which engine answered
([ADR-0005](../adr/0005-module-layout.md)).

Your adapter translates one engine's API into `SpeechProvider`. Everything above
the contract is already written and assumes nothing about you.

## The contract

An adapter implements four methods and one state stream:

| Member | What it does |
|---|---|
| `prepare()` | Load the model and engine. Move `ProviderState` to `PREPARING`, then `READY` or fail. |
| `transcribe(request)` | Return a **cold** `Flow<SpeechEvent>` for one request. Cancellation of the collecting coroutine cancels transcription. |
| `health(): ProviderHealth` | Return the current readiness verdict from cached local state. See below. |
| `close()` | Release native resources. Move `ProviderState` to `CLOSED`. |
| `state: StateFlow<ProviderState>` | `NOT_PREPARED` → `PREPARING` → `READY` → `CLOSED`. The only reactive surface in V1. |

### `SpeechEvent`

The stream emits a sealed set, and the shape of the set is what makes a provider
usable:

- `Preparing` — the engine is warming up.
- `Listening` — audio is arriving and being decoded.
- `Partial(text)` — a **cumulative snapshot** of the utterance so far. Each one
  replaces the last; it is not a delta. Downstream these are conflated (keep
  latest), so emitting them at your engine's natural cadence is fine.
- `Final(text, startedAtMs, endedAtMs)` — the utterance is complete. Utterance
  level, not word level, in V1. Never dropped.
- `Failure(reason, recoverable)` — see `FailureReason` below.

`Partial`, `Final`, and `Failure` are the events the rest of the app reasons
about. `Preparing` and `Final` and `Failure` are never dropped downstream, so
the ordering guarantee you owe is simply that a request produces zero or one
`Final` or `Failure` to terminate it.

**`recoverable` means the dictation can still complete by another route** — not
that this call will succeed. `OfflineModelMissing` is recoverable, because the
router falls back to the next eligible provider and the "download the speech
model" prompt belongs on the user's screen only once every candidate has failed.
Getting this backwards strands dictations that could have succeeded.

`FailureReason` is a typed enum, because the router keys fallback off it:
`OfflineModelMissing`, `UnsupportedLanguage`, `AudioCaptureFailed`, `Timeout`,
`Busy`, `Cancelled`, `Unknown`. Prefer a specific reason over `Unknown`; a
generic reason removes a candidate from the fallback chain that it could have
served.

### Endpointing is yours

Deciding when the user stopped talking is the provider's job, not the
controller's: emit `Final` when your engine's endpoint rule fires. The
controller applies a max-duration guard derived from your declared
`maxAudioDurationSeconds`, but it does not decide where an utterance ends.

If your engine is one-shot (decode a complete waveform, emit one result), say so
in `capabilities.offline` and let the router prefer a streaming provider when the
user's situation allows. Do not fake a partial from a completed decode.

## Capabilities drive all app behaviour

There is no `if provider == X` anywhere in this codebase, and there will not be
one. Every behavioural difference between engines is expressed as a declaration:

| Capability | Meaning |
|---|---|
| `streaming` | Emits `Partial` before `Final`. |
| `offline` | Runs with no network. |
| `supportedLanguages` | Language codes you can actually serve. |
| `autoDetectLanguage` | You can detect without being told. |
| `partialTranscripts` | Must emit `Partial`. |
| `timestamps` | `startedAtMs`/`endedAtMs` are real, not synthesised. |
| `confidence` | You produce confidence scores. |
| `vocabularyBias` | You accept a biasing vocabulary. |
| `pricing` | Your `Declared Rate`, or `null`. |
| `maxAudioDurationSeconds` | Your own ceiling, which the controller enforces. |

**A declared capability is binding.** This is `Capabilities Honesty`, and it is
the rule most likely to break the app in ways that are hard to trace: the router
will hand you a request because you said you could serve it, and the UI will
offer the user a toggle because you said you supported it. Declaring something
you do not reliably do is worse than omitting it, because omitting it is a
correct answer and declaring it is a false promise the rest of the app believes.

`pricing` is `null` when you cannot estimate, which is **not** the same as `0`.
`0` means known free and local providers declare it. `null` means unknown, and
defaulting it to zero would make a paid API look free and pass every cost-ceiling
check — see [provider-testing.md](provider-testing.md), which covers the trap.

## `health()` is cached local truth

`health()` is a plain synchronous method that reads what the adapter already
knows: is the model on disk, is the engine loaded, is the config present, what
was the last failure. It does **not** probe, and it must never make a network
call. V1's only consumer is the router, reading one snapshot at `PREPARING`.

Four values, and they mean three different remediations:

| Value | Means | User's next step |
|---|---|---|
| `HEALTHY` | Can serve now. | — |
| `DEGRADED` | Usable but worse. Eligible, but ranked last. | Nothing — it may still be selected. |

Health never empties the candidate set. If excluding every unhealthy provider
would leave the router with nothing, `DEGRADED` entries are admitted anyway and
the decision is recorded as `DEGRADED_LAST_RESORT`. So a degraded adapter can be
chosen without the user doing anything about it — which is the correct outcome
when the alternative is that they cannot dictate at all, and one more reason
`DEGRADED` must mean *usable but worse* rather than *on the way to unavailable*.
| `UNAVAILABLE` | Transient failure. | Retry. |
| `MODEL_MISSING` | Weights absent or unloadable. | Download — never a retry. |

`ProviderHealth` and `ProviderState` are two vocabularies, not one. `READY`
(lifecycle — the app finished preparing) and `HEALTHY` (readiness — the engine
can serve) are different concepts, and no health value mirrors a lifecycle
value. Do not merge them, and do not add a health value that looks like a
lifecycle value. The invariant the Contract Tests enforce is that
`ProviderState.READY` implies health is one of `HEALTHY`, `DEGRADED`, or
`UNAVAILABLE`, so a `MODEL_MISSING` verdict is necessarily `NOT_PREPARED` — you
cannot be prepared and simultaneously missing your model.

## One instance, one model configuration

A provider instance serves one model configuration. Language is per request.
Swapping models means a new instance held by the router, not a mutating
parameter on a live one. This is what lets the router hold several candidates
warm at once.

## The registry entry

Once the Gradle skeleton lands, a provider is complete when `app/` knows about
it. `app/` wires everything with manual constructor injection — there is no DI
framework in V1 — so the registry entry is the one place your adapter is
constructed and handed to the router:

1. Add the module under `providers/<name>/`.
2. Add one registry entry in `app/` that constructs the provider and exposes it
   to the router alongside the existing adapters.
3. Declare capabilities and pricing that reflect the model you actually ship.

If adding your provider requires a new method on `SpeechProvider`, a new
capability, or a change to ADR-0001, stop. Each of those affects every existing
adapter, so "it's a small change" is rarely small — open a proposal instead
([provider-proposal.md](provider-proposal.md)).

## What comes next

- Run the shared suite before you open the pull request:
  [provider-testing.md](provider-testing.md). It is what makes your declarations
  binding rather than aspirational.
- Licensing, model redistribution, and when to propose:
  [provider-proposal.md](provider-proposal.md).
- What the two existing adapters look like in practice:
  [sherpa-onnx.md](sherpa-onnx.md).