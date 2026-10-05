# Model benchmark results

Every measurement this project has taken of a speech model, in one place, so that a reader
can tell **which instrument produced a number** before trusting it.

**No measurement has been taken yet.** Every section below is empty. An empty section is
absence of evidence, not a result, and none of these documents may be cited as a
performance claim — including on the project site, which carries no benchmark figure for
exactly this reason.

Plan and protocol: [`model-selection.md`](model-selection.md). API background:
[`sherpa-onnx.md`](sherpa-onnx.md).

## Three instruments, and why all three are kept

The same candidate models are measured more than once, by different instruments, because
each one answers a different question. Collapsing them into a single number would delete
the most interesting measurement in the set.

| # | Instrument | What it measures | Owner |
| --- | --- | --- | --- |
| 1 | sherpa-onnx's prebuilt demo APKs | sherpa-onnx's own decode loop. Zero code. | [25](../../.scratch/openflow-v1/issues/25-run-model-benchmark.md) |
| 2 | An API-level script over `OnlineRecognizer`/`OfflineRecognizer` + `Vad` | The same loop with this project's VAD and thread model around it. | [40](../../.scratch/openflow-v1/issues/40-build-the-sherpa-benchmark-harness.md) builds it, [41](../../.scratch/openflow-v1/issues/41-take-the-api-level-benchmark-numbers.md) runs it |
| 3 | OpenFlow's own `SpeechProvider` | The shipping number. | [30](../../.scratch/openflow-v1/issues/30-harness-model-benchmark.md), blocked on [42](../../.scratch/openflow-v1/issues/42-implement-the-sherpa-speech-provider.md) |

**The gap between 2 and 3 is the cost of the integration** — the VAD gating the mic stream,
the `OnlineStream` accept/decode loop, endpointing, result mapping and the decode thread.
That gap is the measurement. It cannot be reported until instrument 3 exists.

Sections are never overwritten. A model re-measured later gets a new dated subsection, so
the fact that a number was superseded stays visible — the same rule ticket 25 applies to
flake in the acceptance gate.

## Rules for anything recorded here

- **Name the device.** A model, a chip tier and a thermal state, per
  [`model-selection.md`](model-selection.md)'s protocol. "RTF 0.21" means nothing without
  them.
- **A single device is not a gate result.** The project has one or two Android devices and
  the gate needs three device classes. Coverage is `N/3` and stays visible as such.
- **Do not restate a threshold to fit the hardware.** `model-selection.md`'s pass thresholds
  stand as written; where one cannot be evaluated on the hardware in hand, say so.
- **List what was not run.** A matrix with silent gaps is worse than a short one, because a
  reader cannot tell which is which.
- **No number reaches the project site** until it appears here first.

---

## 1. Demo APKs (sherpa-onnx prebuilt)

*Instrument: sherpa-onnx's own prebuilt APKs, offline, on `arm64-v8a` / `armeabi-v7a` /
`x86_64`. Directional.*

**Not run.** Requires a device.

## 2. API-level script

*Instrument: `OnlineRecognizer`/`OfflineRecognizer` + `Vad` called directly. Directional,
but includes this project's VAD and thread model.*

**Not run.** The harness does not exist yet ([40](../../.scratch/openflow-v1/issues/40-build-the-sherpa-benchmark-harness.md)),
and running it needs a device ([41](../../.scratch/openflow-v1/issues/41-take-the-api-level-benchmark-numbers.md)).

## 3. Through the SpeechProvider

*Instrument: OpenFlow's own `SpeechProvider`. **The shipping number.***

**Cannot run yet.** There is no `SpeechProvider` in code —
[`core/`](../../core/) has no `src/` directory — so there is no integration to measure. See
[42](../../.scratch/openflow-v1/issues/42-implement-the-sherpa-speech-provider.md).

## Device coverage

| Device class | Behavioural profile | Instruments run |
| --- | --- | --- |
| Pixel-like | *unmeasured* | none |
| Samsung-class | *unmeasured* | none |
| Xiaomi-class | *unmeasured* | none |

`0/3` classes. See [the acceptance gate](../quality/acceptance-gate.md) for what a device
class means and why it is defined behaviourally rather than as a brand list.