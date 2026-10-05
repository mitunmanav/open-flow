# Implement the sherpa-onnx SpeechProvider

Type: task
Status: claimed
Blocked by: 04

## Question

Write the code the map has been describing since ticket 04 and no ticket owned: the
`SpeechProvider` contract in `core`, and OpenFlow's first real implementation of it in
`providers/sherpa`.

Right now the contract is prose. `core/` has **no `src/` directory at all** — ADR-0001
describes `SpeechProvider`, `SpeechEvent`, `Capabilities`, `FailureReason`,
`ProviderHealth` and `Pricing`, and none of them exist as code. `providers/sherpa` contains
one file, `SherpaOnnxContractProbe.kt`, whose own doc comment says the real implementation
"is application code and arrives with the feature". This ticket is that arrival.

It blocks [30](30-harness-model-benchmark.md), which cannot measure the integration until
there is an integration to measure. It also blocks the acceptance gate in practice: a gate
whose fourteen scenarios all "start → record → transcribe → clean → insert → recover
cleanly" needs something that transcribes.

## Scope

**In `core`** — the contract exactly as ADR-0001 and ADR-0002 settled it, plus ADR-0001's
later amendment from ticket 22:

- `SpeechProvider` returning a cold `Flow<SpeechEvent>` with a conflated `Partial`, one
  instance per model configuration.
- `SpeechEvent`, `Capabilities`, `FailureReason`, `Pricing` (micros-USD per second, `0`
  meaning free and `null` meaning cannot-estimate — the distinction ticket 22 drew and the
  one `provider-testing.md` warns contributors to get wrong).
- `ProviderHealth` (`HEALTHY` / `DEGRADED` / `UNAVAILABLE` / `MODEL_MISSING`) as a vocabulary
  **separate** from `ProviderState`, with `health()` a plain cached-truth method. Two
  vocabularies, not one — that separation is the decision, and collapsing it is the mistake
  to avoid.
- The sealed dictation states and single event-channel writer from ADR-0002, if and only if
  they belong in `core` rather than in `app`. Settle which as part of this ticket and say
  which in the ADR record; do not leave it ambiguous.

**In `providers/sherpa`** — the implementation doing the real work the demo APKs never show:

- Model configuration and loading, including `MODEL_MISSING` as a first-class state rather
  than a crash.
- Silero VAD gating the microphone stream.
- The `OnlineStream` accept/decode loop and endpointing, on a decode thread — the
  `SherpaOnnxContractProbe` already proves `OnlineRecognizerConfig` and `OnlineStream` are
  resolvable from this toolchain, so the AAR is known-good; what is unproven is everything
  above it.
- Result mapping onto `SpeechEvent`, including the endpoint→`Final` transition.
- Capability reporting that is honest about what the loaded model can actually do.
- **Contract Tests**, from `core`'s `testFixtures` — ADR-0006 settled that they live there
  so what providers assert against is a published artifact, and ticket 24 wrote
  `docs/providers/provider-testing.md` describing what they assert, including the
  `READY`/health invariant. Those tests are the acceptance criterion for this ticket: the
  provider is done when it passes a suite it did not write.

**Where it must not go:** app code contains no `if provider == sherpa` branches — the
capabilities-driven discipline is the whole point of the seam, and this ticket is the first
place it can be violated.

## Explicitly not in scope

- **`FakeProvider`.** `GLOSSARY.md` defines it and the acceptance gate will want it, but it
  is a separate piece of work and no ticket owns it. Flagged, not absorbed.
- The router, the refiner, insertion, the bubble, onboarding — all settled as designs, none
  implemented, none touched here.
- The benchmark harness ([40](40-build-the-sherpa-benchmark-harness.md)), which is a
  separate instrument and must not be folded into the provider as a way of avoiding writing
  the provider.

## Sizing, honestly

This is the largest single piece of application code the project has attempted, and it may
exceed one session. If the session that claims it finds it too large, **split it rather than
truncating it** — `core`'s contract, then the provider's decode loop, then the Contract Tests
— and wire the halves. A provider that compiles but has no Contract Tests has reproduced the
exact defect ticket 27's probe was written to catch, one layer up.

## Done when

- `core`'s contract exists as code, and `providers/sherpa` implements it.
- The shared Contract Test suite from `core`'s fixtures passes against the sherpa provider.
- `./gradlew lint test assembleDebug` is green — and unlike ticket 27's run, this one
  actually **runs** tests, which is the specific thing to verify rather than assume.
- `docs/providers/model-selection.md`'s assumptions about the API are checked against the
  code that now calls it, and any that turn out to be wrong are corrected rather than left
  for a reader to trip over.
