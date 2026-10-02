# Model Selection — sherpa-onnx on Android (OpenFlow V1)

Research for ticket `.scratch/openflow-v1/issues/06-model-benchmarking.md`. Input: `docs/providers/sherpa-onnx.md`. All model sizes/params trace to the sherpa-onnx docs (https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html) and the `csukuangfj` HuggingFace mirrors referenced there.

## Requirements recap

- Mid-range Android (SD 6xx/7xx-class, 4–8 GB RAM, 4 CPU threads for decode).
- English + multilingual coverage.
- Streaming UX (partials) for the main dictation path; offline/simulated-streaming acceptable for a quality fallback.
- VAD in front (Silero), already covered in `sherpa-onnx.md`.
- Small footprint: bundled default plausible on a phone (tens of MB, not hundreds).

## Candidate model families (realistic shortlist)

Sizes below are the int8 variants (the demos' default), encoder+decoder+joiner from the release listings / HF blob sizes.

| Model | Mode | Languages | Size (int8) | Notes |
|---|---|---|---|---|
| `sherpa-onnx-streaming-zipformer-en-20M-2023-02-17` | Online transducer | English | encoder 41 MB + decoder ~0.5 MB + joiner (small) ≈ ~45 MB | Smallest streaming English zipformer; LibriSpeech-trained; listed on the "small models" page for resource-constrained systems. Primary streaming-English V1 candidate. |
| `sherpa-onnx-streaming-zipformer-en-2023-06-26` | Online transducer | English | encoder 68 MB + decoder 1.3 MB ≈ ~70 MB | Larger streaming English; better WER presumably; candidate "quality tier". |
| `sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16` | Online transducer | Chinese + English | ~65 MB total per listing | Smallest bilingual streaming. |
| `sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20` | Online transducer | Chinese + English | encoder 174 MB int8 | Stronger bilingual, heavy; download tier. |
| `sherpa-onnx-streaming-paraformer-bilingual-zh-en` | Online paraformer | Chinese + English | encoder 165 MB + decoder 72 MB ≈ ~237 MB | Alternative streaming bilingual. |
| `sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17` | Offline (VAD-segmented / simulated streaming) | zh, en, ja, ko, yue | model.int8.onnx 239 MB | Free language ID + emotion/event tags; good offline quality; one-Final-per-utterance. |
| `sherpa-onnx-whisper-tiny.en` / `tiny` | Offline | en or multilingual | encoder 12 MB + decoder 105 MB ≈ ~117 MB (int8) | Known WER baseline; en and multilingual variants; same "fake stream" caveat. |
| `sherpa-onnx-moonshine-tiny-en-int8` / `base-en-int8` | Offline | English (v2 adds ar/cn/es/ja/ko/uk/vi) | tiny: encoder 17 MB + cached decoder 43 MB; mobile-focused. | |
| `sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8` | Offline (VAD/simulated) | English | encoder 652 MB + decoder 7 MB ≈ ~660 MB | High quality, cased+punctuated; too large to bundle; optional download tier. |
| `sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8` | Offline | 25 European languages | encoder 652 MB + decoder 12 MB ≈ ~664 MB | Multilingual-quality tier, download only. |
| `silero_vad.onnx` | VAD | — | ~2 MB (see ticket 02) | Bundle always. |

Excluded for V1 defaults: QNN/RKNN NPU builds (behind config until benchmarking, per `sherpa-onnx.md`), FunASR Nano / Qwen3-ASR / Cohere Transcribe / Omnilingual (LLM-scale or niche, no mid-range streaming fit), T-One (Russian only), offline zipformer/conformer EN models (heavier than the streaming alternatives without streaming benefit).

## Recommended V1 defaults (hypothesis to be confirmed by benchmarking)

1. **VAD (bundled):** Silero VAD, ~2 MB, windowSize 512, gate the mic stream.
2. **Streaming English (bundled default):** `sherpa-onnx-streaming-zipformer-en-20M-2023-02-17` int8 (~45 MB). Streaming partials via `OnlineRecognizer` endpoint lifecycle; falls back to hiding behind VAD if CPU-bound.
3. **Streaming multilingual (download):** `sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16` int8 for zh+en; or streaming paraformer bilingual if 20M-class quality is unacceptable.
4. **Quality fallback (offline, simulated streaming via VAD):** English → Whisper `tiny.en` int8 or Moonshine `tiny-en` int8; multilingual → SenseVoice int8 (zh/en/ja/ko/yue). Parakeet-TDT 0.6b v2/v3 held as optional premium download behind benchmarking.
5. **Packaging:** bundle Silero VAD + streaming-zipformer-en-20M; download everything else via `ModelStore` (per ticket 02 design consequences). Never bundle a >200 MB model in assets.

## Device-benchmarking plan

Goal: confirm or revise the recommended defaults per device tier before shipping V1. Actual execution is a later task; this is the plan.

**Matrix:** { en-20M zipformer, en-2023-06-26 zipformer, small-bilingual-zh-en, streaming paraformer bilingual, whisper-tiny.en, moonshine-tiny-en, sensevoice, parakeet-tdt-v2 (en only)} × {3 representative mid-range devices spanning chip tiers: e.g. SD 695-class, Dimensity 700/810-class, and one SD 7xx for headroom} × num_threads ∈ {2, 4}.

**Metrics per run:**
- RTF (inference time / audio duration) on a fixed eval set.
- Time-to-first-partial and partial-update cadence (online models).
- Endpoint-to-Final latency (offline/simulated models, including VAD segmentation cost).
- WER on a fixed set: LibriSpeech test-clean subset for English + a recorded zh/en dictation set captured on-device; report per model.
- Peak RSS and model load time; APK/assets footprint per model choice.
- Battery drain over a 10-minute continuous dictation session (one device, one battery-tier model).

**Pass thresholds (proposed, tuned after first run):** RTF ≤ 0.3 at num_threads=4 on the weakest tier; first partial ≤ 500 ms; Final within ~1 s of endpoint/VAD-segment close; WER regression vs the larger model in family ≤ +2 absolute on test-clean; peak RSS ≤ 1 GB on 4 GB devices.

**Method:** script via the same Kotlin API the provider will use (`OnlineRecognizer`/`OfflineRecognizer` + `Vad`), fixed 16 kHz mono eval set, 5 warmup decodes discarded, median of ≥20 iterations, thermal state noted, `provider="cpu"`, int8 variants only (fp32 as a spot check on one device if RTF fails).

**Decision rule:** pick the smallest model in each family that passes thresholds; escalate to the next size up when WER gap justifies it; gate multilingual default on SenseVoice vs streaming-bilingual streaming-quality comparison. Results, when run, go in `docs/providers/model-benchmark-results.md` and revise the "Recommended V1 defaults" section above.

## Sources

- Model index and small-models page: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html, https://k2-fsa.github.io/sherpa/onnx/pretrained_models/small-online-models.html
- Per-model sizes (file listings): HuggingFace `csukuangfj/sherpa-onnx-*` (parakeet-tdt 0.6b v2/v3, sense-voice, streaming-paraformer-bilingual-zh-en); k2-fsa release docs pages for whisper `tiny.en`, moonshine `tiny-en-int8`/`base-en-int8`, streaming zipformer en-20M/en-2023-06-26/small-bilingual
- VAD distribution and sizes: `docs/providers/sherpa-onnx.md` (Silero ~2 MB, windowSize 512)
- Streaming/offline API semantics and simulated streaming: `docs/providers/sherpa-onnx.md`
