# Build the FakeProvider

Type: task
Status: open
Blocked by: 42, 44

## Question

`GLOSSARY.md` defines `FakeProvider` — *"a test SpeechProvider implementation used to run
the whole dictation pipeline without a real speech engine"* — ADR-0001 settled it as part
of the contract, and no ticket has ever owned building it. Found while scoping
[42](42-implement-the-sherpa-speech-provider.md), which deliberately excludes it.

## Why it earns a ticket rather than a line in someone else's

It is the thing that makes the pipeline testable at all. Every scenario in the acceptance
gate has to start, record, transcribe, clean up, insert and recover — and on a phone with no
model downloaded, or in a unit test, there is nothing to transcribe with. A `FakeProvider`
scripted to emit a known transcript turns "the pipeline works" from a claim into an
assertion, which is why ADR-0001 put one in the contract rather than leaving it to each
provider's tests.

It is also the only way the **mechanism** scenarios ticket 28 added can run somewhere other
than a device class this project does not own — endpointing, timeout, refiner-degrades-to-raw,
insertion failure and recovery are all provider-independent behaviour, and a fake engine is
precisely how you test them without a model.

## Scope

- Implements the same `SpeechProvider` contract as any real provider, so it is subject to
  the same Contract Tests from `core`'s fixtures. A fake that passes because it skips the
  assertions is worse than no fake.
- **Scriptable, and that is the point.** It replays a scripted sequence of `SpeechEvent`s
  — partials, an endpoint, a final, or a typed `FailureReason` — so a test can say "the
  provider failed with `MODEL_MISSING` at `PREPARING`" and assert what the pipeline does.
  It should be able to produce failures on demand, not just successes.
  Added by ticket 44: it must also script a *deliberately slow* outcome — a `Final`
  whose timestamps put Final Latency above the guard's provisional threshold — so
  G15's state transition is testable without slow hardware.
- **A `Capabilities` it does not lie about.** Whatever it reports must be true of itself;
  `provider-testing.md`'s Capabilities Honesty rules apply to it as much as to sherpa-onnx.
- **Free and instant**, so a test never needs a model, a download, or a network.

## Explicitly not in scope

- Emulating a recogniser's *errors* in the ways sherpa-onnx specifically produces them. The
  typed `FailureReason` values are the contract; how a real engine produces them is that
  engine's business.
- Anything the acceptance gate itself needs on a real device. The gate runs on real models
  on real hardware — that is what makes it a gate. The fake is for the test suite around it.

## Done when

- It passes the shared Contract Test suite in `core`'s fixtures.
- At least one test drives the dictation state machine end to end — partials, endpoint,
  final, insert — with no model present, and one drives a typed failure through recovery.
  Those two tests are what make it worth having; a fake with no pipeline test is unused
  code.