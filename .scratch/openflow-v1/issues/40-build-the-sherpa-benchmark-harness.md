# Build the sherpa-onnx benchmark harness

Type: task
Status: claimed
Blocked by: 27

## Question

Write the instrument `docs/providers/model-selection.md` already specifies and nobody has
built: a benchmark entry point in `providers/sherpa` that drives sherpa-onnx's own Kotlin
API — `OnlineRecognizer` / `OfflineRecognizer` + `Vad` — directly, over a fixed eval set.

This is the agent-buildable half of the model-measurement work.
[41 Take the API-level numbers](41-take-the-api-level-benchmark-numbers.md) runs it;
[30 Measure the candidates through the provider harness](30-harness-model-benchmark.md)
later re-measures the same models through OpenFlow's own `SpeechProvider`.

## Why it exists as its own ticket

`model-selection.md` line 57 names this method — *"script via the same Kotlin API the
provider will use"* — and nothing implements it. It was blocked on ticket 27, which landed
the Gradle project. The alternative method named there, driving the measurement through the
provider, is blocked on [42](42-implement-the-sherpa-speech-provider.md), which does not
exist yet. A decision sitting under the acceptance gate should not wait on a build that has
not been written, so the API-level instrument gets built first.

## Scope

Follow the plan already written. `model-selection.md`'s *Device-benchmarking plan* is the
specification; this ticket implements it and does not re-decide it.

- **A benchmark entry point in `providers/sherpa`**, calling the sherpa-onnx API directly.
  Keep it out of the production provider path — it is an instrument, not the app.
- **Protocol, as specified:** fixed 16 kHz mono eval set, 5 warmup decodes discarded,
  median of ≥20 iterations, thermal state recorded, `provider="cpu"`, int8 variants only.
- **Metrics, per the plan's list:** RTF; time-to-first-partial and partial-update cadence
  for online models; endpoint→Final for offline/simulated models including VAD segmentation
  cost; WER on a fixed set; peak RSS; model load time; battery drain over a ten-minute
  session; the `num_threads ∈ {2, 4}` sweep.
- **WER needs chunked simulated streaming** for online models — they cannot be scored on a
  whole-file decode at all, because that measures a different system than the one that ships.
  Get this right or the WER column is meaningless.
- **Matrix:** the eight candidates in `model-selection.md`'s matrix row, so the harness and
  the plan cannot disagree about what is being measured.

## What this ticket does not do

- **It does not produce numbers.** It produces the thing that produces numbers. Running it
  is ticket 41, and running it needs a device no agent session has.
- **It does not decide the model default.** That is ticket 41's output feeding
  `model-selection.md`'s *Decision rule*, and the decision rule needs a tier to measure
  against — see Q4 below.
- **It is explicitly a parallel path.** Once [42](42-implement-the-sherpa-speech-provider.md)
  exists, this harness and the provider will both wrap the same sherpa-onnx API, and only
  ticket 30 measures the real thing. That is the accepted cost of getting a number before
  the provider exists. Share model configuration with the provider where it is cheap to do
  so, so the drift is bounded — but do not contort the instrument into a
  `SpeechProvider` implementation to achieve it.

## The threshold problem, carried forward not solved here

`model-selection.md` sets the bar at *"RTF ≤ 0.3 at `num_threads=4` on the weakest tier"*.
There is no weakest tier: the project has one or two devices. So the harness can report RTF
but cannot pass or fail this bar on its own.

Settled: **the threshold stays as written and stays unmeasured.** It is not redefined to
fit whatever phone is on hand — the map's Notes already rule out quietly rewriting the
promise to match the hardware. It is gated instead by the acceptance gate's existing
"aspirational until hardware exists" mechanism (ticket 28), which is the same mechanism
that already covers the missing device classes. The harness therefore reports RTF and
whether the weakest *available* device met it, and says plainly which device that was. A
number from one phone is not a gate result and must not be labelled as one.

## Done when

- The harness compiles and its entry point runs, on a device, over at least one model —
  or, if no device is available to the session that builds it, it compiles, the CI check
  passes, and the ticket hands [41](41-take-the-api-level-benchmark-numbers.md) a precise
  run procedure rather than closing as though a measurement happened.
- `./gradlew lint test assembleDebug` is green, and the new code is covered by the same
  gate as everything else.
- The run procedure for 41 is written down precisely enough that a human holding a phone
  and an eval set can execute it without reading this ticket's code.