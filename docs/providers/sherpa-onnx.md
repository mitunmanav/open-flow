# sherpa-onnx — Android Provider Research

Research for the SherpaOnnxProvider adapter (OpenFlow V1). All claims trace to primary sources: the sherpa-onnx GitHub repo (k2-fsa/sherpa-onnx) and the official docs at k2-fsa.github.io/sherpa.

## What it is

- C++ ASR/TTS/VAD framework wrapping Next-gen Kaldi + onnxruntime, no network needed at inference. Repo: https://github.com/k2-fsa/sherpa-onnx
- Android consumption path: JitPack artifact `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:<tag>` (current docs use `v1.13.5`), which bundles the native `sherpa-onnx-jni` library for `arm64-v8a` (and other ABIs) — no NDK build needed for consumers. The Kotlin/Java class files (`OnlineRecognizer`, `OfflineRecognizer`, `Vad`, …) are copied from `sherpa-onnx/kotlin-api/` into the consuming app, or the equivalent `java_api` sources.
- Package name: `com.k2fsa.sherpa.onnx` for Kotlin API; Java demos use the same JNI surface.

## Streaming vs non-streaming ASR

Two recognizer classes covering the whole matrix:

| Mode | Kotlin class | Stream semantics | Model types used |
|---|---|---|---|
| Online (streaming) | `OnlineRecognizer` + `OnlineStream` | Feed chunks via `stream.acceptWaveform`, `decode`, `getResult` yields partial text that can be replaced until an endpoint; `isEndpoint`/`reset` implement endpointing | Zipformer transducer (online), streaming Zipformer2 CTC, streaming Paraformer, NeMo streaming Fast-Conformer CTC, T-One CTC, Nemotron streaming |
| Offline (non-streaming) | `OfflineRecognizer` + `OfflineStream` | Accept full waveform, single `decode`, one result | Zipformer transducer (offline), Paraformer, Whisper, NeMo CTC/transducer/TDT, Canary, SenseVoice, Moonshine, Omnilingual, Wenet/Zipformer-CTC, FireRedAsr, Dolphin, MedASR, FunASR Nano, Qwen3-ASR, Cohere Transcribe, TeleSpeech |

Key implication for the provider contract: sherpa-onnx streaming ASR emits **mutable partial text** (each `getResult` after `decode` may differ; final emerges around endpoint), whereas offline is one-shot. The SpeechProvider's `Partial`/`Final` event split maps naturally to the online-recognizer endpoint lifecycle — not to offline decoding.

Endpointing is built into the online API: `EndpointConfig` (rule1/rule2/rule3 with `minTrailingSilence`, `minUtteranceLength`, defaults 2.4s/1.4s, 20s max) with `enableEndpoint=true` by default. After `isEndpoint == true` you take the result and `reset(stream)` for the next utterance.

**Simulated streaming**: offline models (Whisper, Moonshine, SenseVoice, Parakeet-TDT, etc.) run in a "simulated streaming" loop — decode completed VAD segments or fixed windows incrementally. Prebuilt APK exists (`SherpaOnnxSimulateStreamingAsr`). This is the only way to get Whisper-family output with near-real-time UX on Android.

**Two-pass**: `SherpaOnnx2Pass` demo = online model for partial + offline model for final rescoring of each endpoint segment.

## VAD

- `Vad` class in `Vad.kt`; models: Silero VAD (`silero_vad.onnx`, windowSize 512) and TEN VAD (`ten-vad.onnx`, windowSize 256), both ONNX, sample rate 16000, threshold/minSilence/minSpeech/maxSpeechDuration configurable.
- Loop: `acceptWaveform(samples)` → while `!empty()`: `front()` gives `SpeechSegment(start, samples)`, `pop()`. `isSpeechDetected()` for partial speech state. `flush()` forces a segment at stream end.
- Distribution: models live as release assets (e.g. `silero_vad.onnx` under release `asr-models`) and are pulled at runtime; canonical APK pairing: VAD alone, or VAD + offline ASR (`SherpaOnnxVadAsr`) which chops mic audio into segments that feed `OfflineRecognizer`.

## Supported model families (ASR)

From `OfflineModelConfig` / `OnlineModelConfig` in the Kotlin API:

- **Zipformer** (transducer, online + offline; multiple sizes, int8/fp32/fp16 variants)
- **Zipformer2 CTC** (online + offline)
- **Paraformer** (online + offline, zh-centric)
- **Whisper** (offline only; multilingual + `.en` variants; `language`, `task=transcribe|translate`, token timestamps)
- **NeMo**: Fast-Conformer CTC, CitriNet, TDT (`nemo_transducer`, e.g. Parakeet-TDT 0.6b v2/v3, Parakeet-unified), Giga-AM (ru), streaming Fast-Conformer CTC, Canary (encoder/decoder with srcLang/tgtLang, e.g. canary-180m-flash)
- **SenseVoice** (zh/en/ja/ko/yue, inverse-text-normalization flag)
- **Moonshine** (tiny/base; v1 preprocessor+encoder+cached/uncached decoder, v2 merged decoder)
- **Omnilingual ASR** (1600 languages, 300M CTC)
- **Wenet/Zipformer-CTC**, **FireRedAsr (v1/v2)**, **Dolphin**, **MedASR**, **FunASR Nano** (LLM-based), **Qwen3-ASR**, **Cohere Transcribe**, **T-One**
- QNN/Snapdragon NPU variants (`provider = "qnn"`, models shipped as `libmodel.so` + `libQnnHtp.so`), RKNN variants for RK3588.

## Kotlin/Java API surface (the adapter-relevant bits)

```kotlin
// Online
val recognizer = OnlineRecognizer(assetManagerOrNull, OnlineRecognizerConfig(
  featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
  modelConfig = OnlineModelConfig( /* transducer/paraformer/zipformer2Ctc/neMoCtc/toneCtc paths, tokens, numThreads, provider="cpu", modelType */ ),
  endpointConfig = getEndpointConfig(), enableEndpoint = true,
))
val stream = recognizer.createStream()
stream.acceptWaveform(samples)         // FloatArray, 16 kHz mono
while (recognizer.isReady(stream)) recognizer.decode(stream)
if (recognizer.isEndpoint(stream)) { val r = recognizer.getResult(stream); /* text, tokens, timestamps, ysProbs */ }
recognizer.reset(stream); stream.release? // see OnlineStream
recognizer.release()

// Offline
val asr = OfflineRecognizer(null, OfflineRecognizerConfig(modelConfig = OfflineModelConfig( /* whisper/paraformer/nemo/... */ ), featConfig = FeatureConfig()))
val s = asr.createStream()
s.acceptWaveform(samples); asr.decode(s)
val r = asr.getResult(s)   // text, tokens, timestamps, lang, emotion, event, durations (TDT)
```

- Results carry `text`, `tokens`, `timestamps` (FloatArray seconds), and for offline `lang`/`emotion`/`event` (SenseVoice) and per-token `durations` (TDT). No word-level confidence; `ysProbs` exists on online results.
- Hotwords/contextual biasing: `hotwordsFile` + `hotwordsScore` on configs, `createStream(hotwords = ...)`.
- Rule FSTs (`ruleFsts`/`ruleFars`) for text normalization/injection; homophone replacer config.
- Java API exists (`SherpaOnnxJavaDemo`), same JNI layer, Groovy/Kotlin DSL demos.
- VAD, TTS (`Tts`), punctuation, KWS, speaker diarization/id, spoken-language-id, speech denoise have equivalent Kotlin classes.

Threading/lifecycle notes:
- Everything is call-driven on the calling thread; no async callbacks. The adapter should run decode loops on a dedicated worker (the demos use a coroutine/Thread per recognition session).
- `release()` must be called on recognizer and stream eventually; JNI class loads `System.loadLibrary("sherpa-onnx-jni")` in companion init — safe to load once.
- Models can be loaded from app `assets/` (`newFromAsset`) or from filesystem paths (`newFromFile`) — decision point for downloaded-vs-bundled models.

## Model packaging & download

- Official path: download `.tar.bz2` per model from `https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/<name>.tar.bz2` (mirrors on HuggingFace `csukuangfj/sherpa-onnx-*`), extract, point the config at the directory.
- Demos bundle models in `app/src/main/assets/` (APK size grows by the model footprint — whisper-tiny int8 ≈ 40 MB, base ≈ 75 MB, zipformer small models tens of MB; Silero VAD ~2 MB).
- Packaging options for OpenFlow:
  1. Bundle a default V1 model in `assets/` — simplest, offline-first, but +30–100 MB APK.
  2. Download on first launch to app-internal storage and use `newFromFile` paths — smaller APK, needs download manager + integrity check + a `MODEL_MISSING` Provider Health state in `SpeechProvider.health()` (ADR-0001), distinct from `DEGRADED` because its fix is a download prompt rather than a retry.
  3. Hybrid: bundle VAD (tiny), download ASR model(s) on demand.
- No official runtime downloader in the Android artifact; you write the fetch/verify/extract code yourself. iOS/macOS use the same manual approach.

### Measured archive layout (ticket 46)

The `asr-models` release assets were fetched and measured, not read from
prose. For the V1 default, `sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2`
(127,887,156 bytes, SHA-256 `9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9`,
pinned in `providers/sherpa`'s `modelstore/ModelSpec.kt`):

- One top-level directory named after the model, containing **both** fp32
  and int8 variants: `encoder-epoch-99-avg-1.onnx` and
  `encoder-epoch-99-avg-1.int8.onnx`, likewise
  `decoder-epoch-99-avg-1(.int8).onnx` and
  `joiner-epoch-99-avg-1(.int8).onnx`. The entry names are the
  `-epoch-99-avg-1` forms — not an `encoder.int8.onnx` shorthand — so
  model config paths must be taken from the spec, not typed from memory.
- `tokens.txt`, `README.md`, an export script, and a `test_wavs/`
  directory.
- OpenFlow extracts only the int8 trio and `tokens.txt` (~45 MB of the
  ~210 MB unpacked archive); the archive as a whole remains the unit of
  integrity, because the pinned SHA-256 covers every byte of it. The
  pinned set lives in `modelstore/ModelSpec.kt`.

## Prebuilt Android demo lessons

| Demo (android/) | Teaches |
|---|---|
| `SherpaOnnx` | The main Kotlin streaming-ASR demo: asset extraction to files, OnlineRecognizer lifecycle, mic record → acceptWaveform → decode loop, endpoint reset |
| `SherpaOnnxVadAsr` | VAD-segmented audio feeding non-streaming ASR — the template for "VAD + offline model" dictation |
| `SherpaOnnxVad` | VAD-only: Silero config, segment extraction |
| `SherpaOnnxSimulateStreamingAsr` | Offline model (Whisper/SenseVoice/Moonshine/Parakeet) driven chunks-wise for streaming UX |
| `SherpaOnnx2Pass` | Online partial + offline rescoring per endpoint |
| `SherpaOnnxJavaDemo` / `...WearOs` | Java consumers, WearOS Compose |
| `SherpaOnnxTts(Engine)`, `Kws`, `Speaker*`, `WebSocket` | Out of scope but show the same Kotlin-API packaging pattern |

Common demo pitfalls to avoid copying: models extracted into `getExternalFilesDir`, relative asset paths, no backpressure handling on decode loops, endpoint config left at defaults even for short dictation utterances.

## Quantization

- Shipped per-model as separate files: `*.int8.onnx` (dynamic quantization), `*.onnx` (fp32), `*.fp16.onnx` (some Zipformer2). Picking int8 usually cuts size ~4× with minor WER cost; demos generally default to int8 encoder + fp32 decoder/joiner.
- No runtime quantization flag; quantization is a property of the shipped model file. So "quantization option" in OpenFlow = choosing which prebuilt variant to download per device tier (a router/benchmarking input, see ticket 06).
- QNN/RKNN are the device-specific "quantization+NPU" escape hatch for Snapdragon/RK3588.

## Design consequences for SherpaOnnxProvider

1. Implement streaming path with `OnlineRecognizer` + built-in endpointing; map `getResult` before endpoint → `Partial`, endpoint result → `Final`. This is the natural V1 path (Zipformer transducer or streaming Paraformer).
2. Offer Whisper-class quality via either (a) offline decode of a VAD-delimited utterance, or (b) simulated-streaming — both are "fake stream" from the router's perspective: one `Final` per endpoint, no true partials. Encode this in `capabilities` rather than a fake streaming flag.
3. Run VAD (`Vad`, Silero) in front to gate/skip silence and to bound max utterance length; `Vad.flush()` handles tail.
4. Model lifecycle: start with bundled default in assets; abstract behind a `ModelStore` so downloads can be added. Keep `newFromAsset` vs `newFromFile` both supported.
5. One decode thread per active session; no callbacks — translate pull-loop into the provider's event Flow/StateFlow.
6. Expose `provider="cpu"` default; leave QNN/RKNN behind config until benchmarking.
7. Result mapping: `text` → transcript; `timestamps` available if we later need word timing; SenseVoice gives `lang`/`emotion`/`event` for free; no confidence → router treats confidence as N/A.
8. Lifecycle: `release()` on both stream and recognizer in `close()`; guard JNI library double-load.

## Sources

- Kotlin API sources: https://github.com/k2-fsa/sherpa-onnx/tree/master/sherpa-onnx/kotlin-api (OnlineRecognizer.kt, OfflineRecognizer.kt, Vad.kt)
- Java for Android (JitPack setup, model download steps): https://k2-fsa.github.io/sherpa/onnx/java-api/anroid-java.html
- Android section + prebuilt APKs: https://k2-fsa.github.io/sherpa/onnx/android/index.html, https://k2-fsa.github.io/sherpa/onnx/android/prebuilt-apk.html
- Android demos: https://github.com/k2-fsa/sherpa-onnx/tree/master/android
- Model index: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html (+ whisper/, nemo/, paraformer/ pages)
- VAD: https://k2-fsa.github.io/sherpa/onnx/vad/index.html
