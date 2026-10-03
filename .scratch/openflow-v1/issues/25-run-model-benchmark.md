# Measure the model candidates on the devices we actually have

Type: task
Status: claimed
Blocked by: none

> **Waiting on a human with hardware.** No agent session can finish this one — it is
> measured by running APKs on a phone. It stays `claimed` so it does not sit on the
> frontier pretending to be takeable. The map's Notes carry the reasoning.

## What changed, and why this ticket was rewritten

The original framing was "run the matrix in `docs/providers/model-selection.md`, which
gates on *RTF ≤ 0.3 at `num_threads=4` on the weakest tier*." Two facts broke that:

1. **There is no weakest tier to measure.** The owner has one or two real devices, not the
   three the matrix assumes. The threshold that was supposed to *confirm or kill* the
   Silero + zipformer-en-20M int8 default has nothing to be evaluated against. So the
   default cannot be confirmed by this ticket, and it cannot be refuted either — which is
   the honest state, and it is the state the map should have recorded earlier.
2. **The stated method was blocked on code that does not exist.** "Script via the same
   Kotlin API the provider will use" needs ticket 27's Gradle skeleton, the sherpa-onnx
   JitPack dependency, and a written harness. This repo contains zero application code.
   Under the original framing nothing could be measured for a long time, which is the
   worst possible property for the decision sitting underneath the acceptance gate.

The way out is that **sherpa-onnx publishes prebuilt demo APKs for the exact matrix**, so
the first measurement needs no code at all. Verified against the upstream prebuilt-APK
index while rewriting this ticket.

## What is measurable today, with no code

From the upstream [prebuilt APK index](https://k2-fsa.github.io/sherpa/onnx/android/prebuilt-apk.html)
(demos "run locally, without internet connection"), there is an APK for **the exact V1
default model**, built for `arm64-v8a`, `armeabi-v7a`, and `x86_64`:

| APK (on the `csukuangfj2/sherpa-onnx-apk` HF repo, `asr/1.13.8/`) | Model it carries | Matrix row |
|---|---|---|
| `sherpa-onnx-1.13.8-arm64-v8a-asr-en-small_zipformer_20M_2023_02_17.apk` | `sherpa-onnx-streaming-zipformer-en-20M-2023-02-17` | **the V1 default under test** |
| `…-asr-en-zipformer2.apk` | larger streaming zipformer | en-2023-06-26 quality tier |
| `…-asr-bilingual_zh_en-zipformer.apk` | streaming bilingual zh+en | multilingual streaming |
| `…-asr-zh_en-paraformer.apk` | streaming paraformer bilingual | alternative streaming bilingual |
| simulated-streaming family (`apk-simulate-streaming-asr.html`) | whisper-tiny.en, moonshine-tiny, sense-voice, parakeet | offline quality fallback + VAD segmentation cost |
| VAD family (`apk-vad-asr.html`, `apk-vad.html`) | Silero + offline ASR | VAD overhead, endpoint→Final latency |

Drift worth noting: upstream APKs are at `1.13.8`, while `docs/providers/sherpa-onnx.md`
records the JitPack tag as `v1.13.5`.

## The procedure

One device to start. Repeat on the second if there is one. For each model:

1. Install the model-specific APK for the device's ABI.
2. **Confirm once, on the device, whether the APK ships its model baked in or expects a
   push.** This is the one step not verifiable from the docs. If it expects a push, use the
   demo's own convention (`sherpa-onnx.md` records models extracted into
   `getExternalFilesDir`), from the `asr-models` release tarball.
3. Grant `RECORD_AUDIO` when prompted.
4. Record one fixed clip at 16 kHz, the **same** clip for every model — ideally read from
   the same sentence file, so differences are the model's and not the speaker's.
5. Capture, verbatim: **RTF**, **time to first partial**, **endpoint→Final latency**, and
   the device's SoC / RAM / thermal state at the run. `adb logcat` is the reliable source
   when the on-screen readout is unclear. Note `num_threads` if the demo exposes it.
6. Repeat the same clip **≥5 times and take the median**, not a single run — one cold run
   measures model load, not inference.

Stop at step 6 for the first pass. WER, peak RSS, model load time, battery drain, and the
`num_threads ∈ {2,4}` sweep are deliberately *not* in this pass: they need the harness, and
faking them from a demo UI produces numbers that look authoritative and are not.

## Deliverables

- `docs/providers/model-benchmark-results.md`, new, listing the devices actually used, the
  SoCs, the models measured, and the medians. **State plainly at the top which parts of the
  plan this pass did not cover** — weakest tier, WER, RSS, threads — so a later reader
  cannot mistake directional numbers for the gate.
- Add it to `docs/README.md` in the same commit, or `docs-check` fails it as an orphan.
- Revise the "Recommended V1 defaults" section of `model-selection.md` **only where the
  measurements actually bear on it.** If 20M int8 clears comfortably on the hardware in
  hand, that is evidence about *those devices* and no evidence about the weakest tier the
  acceptance gate names. Say exactly that. If it fails, that is a real finding and the ADR
  and map entry recording the default get amended rather than a table quietly edited.

## What this ticket cannot settle

It cannot confirm the V1 default for the destination's three-device gate. Nothing measured
on one or two devices can. The compensating control — making a slow device degrade at
runtime instead of stalling — is its own decision, tracked as
[31 The runtime RTF guard](31-runtime-rtf-guard.md), and is takeable now precisely because
it does not wait on numbers.

The harness-based measurement that *does* measure the real integration path, through the
Kotlin API the provider will actually ship with, is split out as
[30 Measure the candidates through the provider harness](30-harness-model-benchmark.md),
blocked on ticket 27.