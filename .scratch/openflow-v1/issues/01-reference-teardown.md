# Reference app teardown

Type: research
Status: resolved
Blocked by: none

## Question

Distill the architecture of vox-android, OpenWhispr, phone-whisper, Sayboard, and WhisperInput into a single reference doc (`docs/architecture/reference-apps.md`): their bubble→STT→cleanup→insertion flow, state handling, permissions approach, and license compatibility with Apache-2.0. Which parts teach us and which parts must we avoid copying?

## Answer

Reference doc written: `docs/architecture/reference-apps.md` (comparison table + per-app teardown + cross-app synthesis + license matrix).

Key findings:

- **Flow consensus** among the bubble apps (vox-android, phone-whisper, OpenWhispr): floating overlay → tap-to-record → tap-to-stop → STT (whisper.cpp, sherpa-onnx, or cloud Whisper) → optional cleanup (on-device Gemma / OpenAI / Groq) → AccessibilityService insertion into the focused field, with **clipboard copy as the insertion fallback**. Sayboard and WhisperInput take the IME/RecognitionService route instead — a different surface we should not adopt for V1.
- **State handling**: vox-android's published single-threaded `IDLE → RECORDING → PROCESSING` state machine with lazy model load + idle unload is the cleanest pattern; phone-whisper/OpenWhispr add foreground-service persistence, battery-exemption prompting, and a pause switch that avoids revoking accessibility. WhisperInput documents the "model-not-ready, first take inconsistent" pitfall we should design around.
- **Permissions**: every app converged on the same triad — RECORD_AUDIO + display-over-other-apps (bubble apps only) + Accessibility Service, with a guided walkthrough and a scoped "why accessibility" explanation. OpenWhispr's Android 13+ restricted-settings walkthrough and QUERY_ALL_PACKAGES workaround notes are worth keeping.
- **License**: vox-android MIT, WhisperInput MIT (deps Apache-2.0), phone-whisper and OpenWhispr Apache-2.0 — all compatible with OpenFlow's Apache-2.0. **Sayboard is GPL-3.0: exclude its code entirely** (architecture read only; Vosk engine itself is Apache-2.0). Avoid copying its IME-first pattern and its vosk-android-demo lineage regardless.
- **Teach vs avoid**: teach = bubble UX, three-signal focus detection, insertion-with-clipboard-fallback, battery/OEM keepalive handling, model lifecycle. Avoid = cloud-default vendor-baked cleanup prompts (V1 refiner is deterministic), in-app APK self-update, IME-as-primary input, Sayboard's copyleft code, and bundling model binaries.
