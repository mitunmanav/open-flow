# Correct the false claims in the sherpa-onnx provider docs

Type: task
Status: open
Blocked by: none

## Question

`docs/providers/sherpa-onnx.md` and `docs/providers/model-selection.md` describe an
API that does not exist. Verified against the upstream source while building the
Gradle skeleton, where the wrong claims were load-bearing: a reader following them
writes code that does not compile.

Four corrections:

- **The class is `OnlineRecognizer`, not `SherpaOnnxRecognizer`.** The old name is
  gone from the tree; `sherpa-onnx/kotlin-api/SherpaOnnxRecognizer.kt` is a 404.
  The real types are `OnlineRecognizer`, `OnlineStream`, `OfflineRecognizer`,
  `OfflineStream`, `Vad`, all in package `com.k2fsa.sherpa.onnx`.
- **The Kotlin API ships *inside* the AAR.** The docs say the sources are "copied
  from `sherpa-onnx/kotlin-api/` into the consuming app". That was true before an AAR
  existed. Upstream now symlinks those files into the AAR module, so the classes are
  in the dependency and copying them is both unnecessary and wrong.
- **Silero VAD is 629 KB, not ~2 MB** (`silero_vad.onnx`, 643,854 bytes; the int8
  variant is 208 KB). Overstated by ~3× in both documents.
- **The version is `v1.13.8`, not `v1.13.5`.** Three releases stale. Upstream's own
  docs page is stale too, which is presumably where the number came from.

Also worth correcting while in there: `OnlineStream.acceptWaveform` takes **two**
arguments (`samples`, `sampleRate`), and the asset-vs-file choice is made by whether
the `AssetManager` constructor argument is null — `newFromAsset` and `newFromFile` are
private externals, not methods you call.

Do not touch the endpoint defaults (2.4 s / 1.4 s / 20 s) — those were checked against
the real `EndpointConfig` and are correct.

The model figures in `model-selection.md` are **right**: the four files that actually
ship are 45,202,074 bytes, which is the "~45 MB" the doc claims. Leave that alone.

## Answer