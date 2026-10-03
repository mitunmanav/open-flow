# Measure the candidates through the provider harness

Type: task
Status: open
Blocked by: 27

## Question

Split out of [25 Measure the model candidates on the devices we actually have](25-run-model-benchmark.md),
which measures the same models through sherpa-onnx's prebuilt demo APKs because no
application code exists yet. That is the right first measurement and the wrong final one.

Why it is still owed: a demo APK measures sherpa-onnx's own decode loop, not OpenFlow's.
The shipping number comes from the provider doing the real work around it — VAD gating the
mic stream, the `OnlineStream` accept/decode loop, endpointing, result mapping, the decode
thread. Those add latency the demo never shows, and `model-selection.md` was right to name
the Kotlin API as the method. This ticket is where that method is actually spent.

Once ticket 27 lands there is a Gradle project and a `providers/` module to hang it off, so
the shape is: a benchmark entry point in the provider module, driven through the same
`SpeechProvider` surface the app uses, so it measures the integration rather than a
parallel path that can drift from it.

What it settles that ticket 25 cannot:

- **The remaining plan items**, which need a harness and a real UI to read: WER against
  LibriSpeech test-clean (online models need chunked simulated streaming to be scored at
  all), peak RSS, model load time, battery drain over a ten-minute session, and the
  `num_threads ∈ {2, 4}` sweep.
- **First-partial and endpoint→Final latency measured through the provider**, including the
  VAD cost the demo's VAD APK reports separately.
- **The device matrix**, as far as hardware ever allows. If this is still one or two
  devices in V1, say so in the results document rather than presenting a single-device
  number as the gate.

Record results in the same `docs/providers/model-benchmark-results.md` ticket 25 creates, as
a clearly separate section — directional demo numbers and integration numbers are not the
same measurement, and overwriting one with the other loses the fact that both were taken.

Do not resolve this by copying ticket 25's numbers forward. The point is that they were
measured on a different path.