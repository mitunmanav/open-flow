# sherpa-onnx on Android

Type: research
Status: resolved
Blocked by: none

## Question

What does sherpa-onnx offer on Android today: streaming vs non-streaming ASR, VAD, supported model families (Zipformer, Paraformer, Whisper, NeMo/Canary, etc.), Kotlin/Java API surface, model download/packaging, prebuilt Android demo lessons, and quantization options? Capture in `docs/providers/sherpa-onnx.md`, enough to design the SherpaOnnxProvider adapter.

## Answer

sherpa-onnx on Android ships a Kotlin/Java JNI layer (`com.k2fsa.sherpa.onnx`, packaged via JitPack `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.5`, native lib `sherpa-onnx-jni`). Two ASR entry points: `OnlineRecognizer`/`OnlineStream` (streaming: Zipformer transducer, streaming Paraformer, NeMo streaming CTC, zipformer2-CTC, T-One; built-in endpointing rules, partial results replaced until endpoint) and `OfflineRecognizer`/`OfflineStream` (one-shot: Zipformer transducer, Paraformer, Whisper, NeMo CTC/TDT/Canary, SenseVoice, Moonshine, Omnilingual, FireRedAsr, Dolphin, Wenet-CTC, MedASR, FunASR Nano, Qwen3-ASR, Cohere Transcribe). VAD via `Vad` class (Silero or ten-vad ONNX models, 16 kHz, segment extraction with max speech duration). Whisper-class quality gets real-time UX through "simulated streaming" (VAD/windowed offline decode) — same for two-pass (online partial + offline rescoring). Models downloaded as per-model `.tar.bz2` from the `asr-models` release tag or bundled in `app/src/main/assets`; configs load from assets or filesystem (`newFromAsset`/`newFromFile`). Quantization is a property of the shipped file (`.int8.onnx` dynamic, fp32, some fp16) — int8 default in demos; QNN/RKNN variants exist for NPU/RK3588. Prebuilt APKs and Android demos under `android/` (SherpaOnnx, SherpaOnnxVad, SherpaOnnxVadAsr, SherpaOnnxSimulateStreamingAsr, SherpaOnnx2Pass, SherpaOnnxJavaDemo) teach the asset-extract → OnlineRecognizer loop → endpoint reset pattern. Full write-up for adapter design: `docs/providers/sherpa-onnx.md` — key consequences: true streaming (online recognizer) maps Partial/Final naturally, Whisper/etc. is one-Final-per-endpoint via VAD; run Silero VAD in front; abstract model storage for bundled-vs-downloaded; dedicated decode thread; release() everything in close().
