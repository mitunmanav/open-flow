# First sherpa model selection

Type: research
Status: resolved
Blocked by: 02

## Question

Which sherpa-onnx model(s) are realistic defaults for OpenFlow V1 on mid-range Android (latency, size, English + multilingual, streaming)? Design the benchmark plan; actual execution may be a later task. Capture in `docs/providers/model-selection.md`.

## Answer

Full findings in `docs/providers/model-selection.md`.

Recommended V1 defaults (hypotheses pending the benchmark run):
- VAD: Silero, bundled (~2 MB).
- Streaming English: `sherpa-onnx-streaming-zipformer-en-20M-2023-02-17` int8 (~45 MB), bundled; `sherpa-onnx-streaming-zipformer-en-2023-06-26` int8 (~70 MB) as the quality-tier alternative.
- Streaming multilingual: `sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16` int8, download tier; streaming paraformer bilingual (~237 MB int8) as heavier alternative.
- Offline/simulated-streaming quality fallback: Whisper tiny.en/tiny int8 (~117 MB) for English, Moonshine tiny-en int8, SenseVoice int8 (239 MB, zh/en/ja/ko/yue) for multilingual; Parakeet-TDT 0.6b v2/v3 int8 (~660 MB) as premium download, not bundled.
- Packaging: bundle VAD + streaming-en-20M only; everything else via ModelStore downloads; nothing >200 MB in assets.

Benchmark plan: model × 3 mid-range chip tiers × num_threads {2,4}, measuring RTF (≤0.3 @ 4 threads), time-to-first-partial (≤500 ms), endpoint-to-Final latency, WER on fixed LibriSpeech-subset + on-device dictation set, peak RSS, load time, battery over 10-min session; pick the smallest model meeting thresholds per tier/language; results feed back into `model-selection.md`.

