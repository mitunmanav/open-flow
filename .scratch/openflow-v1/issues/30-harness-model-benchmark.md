# Measure the candidates through the provider harness

Type: task
Status: open
Blocked by: 27, 42

## Question

Measure the same models as [25](25-run-model-benchmark.md) and
[41](41-take-the-api-level-benchmark-numbers.md), but through OpenFlow's own
`SpeechProvider` rather than sherpa-onnx's decode loop. This is the shipping number.

Why it is still owed: a demo APK measures sherpa-onnx's own loop, and an API-level script
measures its loop with our VAD and thread model wrapped around it. Neither shows what the
provider does — VAD gating the mic stream, the `OnlineStream` accept/decode loop,
endpointing, result mapping, the decode thread. Those add latency, and the model default
should be chosen against the number the app will actually produce.

What it settles that 25 and 41 cannot:

- **The integration latency delta** — first-partial and endpoint→Final *through* the
  provider, with the VAD cost in the same measurement rather than reported beside it.
- **Whether the shipped default survives its own integration.** A model that passes RTF at
  the API level can still miss the first-partial budget once a `Flow<SpeechEvent>` is in
  the path. Only this ticket can fail that way.
- **The device matrix**, as far as hardware ever allows. If this is still one or two
  devices in V1, say so in the results document rather than presenting a single-device
  number as the gate.

Record results in `docs/providers/model-benchmark-results.md`, as a third clearly separate
section. Three instruments now report on these models — the demo APKs (25), the API-level
script (41), and the provider (this ticket) — and overwriting one with another loses the
fact that all three were taken. The section states which instrument produced it.

Do not resolve this by copying 25's or 41's numbers forward. The point is that they were
measured on a different path.

## Why this ticket was blocked, and what changed

An earlier draft of this ticket assumed that once ticket 27 landed there would be a
`providers/` module to hang a harness off, and that the harness could be "driven through
the same `SpeechProvider` surface the app uses". Both halves of that were wrong when this
ticket was picked up:

- **There is no `SpeechProvider` in code.** `core/` has no `src/` directory at all. The
  contract exists as prose in ADR-0001 and nothing else; the only Kotlin file in the repo
  is `SherpaOnnxContractProbe.kt`, which says outright that the real implementation
  "is application code and arrives with the feature". No ticket owned writing it.
- **`model-selection.md` names a different method.** Its *Method:* line specifies a script
  over `OnlineRecognizer`/`OfflineRecognizer` + `Vad` — the sherpa-onnx Kotlin API — not a
  drive through `SpeechProvider`. This ticket contradicted the document it claimed to
  execute.

So the ticket was re-scoped rather than closed. Its premise was real and its blocker was
undeclared. Two tickets came out of it: [41 Take the API-level numbers](41-take-the-api-level-benchmark-numbers.md)
builds and runs the instrument `model-selection.md` already specifies, and
[42 Implement the sherpa-onnx SpeechProvider](42-implement-the-sherpa-speech-provider.md)
owns the missing implementation this ticket needs. `Blocked by: 42` is new and load-bearing.

The pass threshold stays as `model-selection.md` states it — RTF ≤ 0.3 at `num_threads=4`
on the weakest tier — and stays **unmeasured**, because there is no weakest tier to measure
it on. It is gated by the acceptance gate's existing "aspirational until hardware exists"
mechanism (ticket 28), not by a threshold quietly rewritten to fit one phone.