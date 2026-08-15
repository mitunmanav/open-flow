# Architecture inventory — 2026-08-15

**Lane:** `main` @ `ce4b85b` · cloud ears **merged** (OkHttp WS + PCM)

## Layer map

```text
UI (Compose MainActivity / shell / engine picker)
 ↓
Presentation (FlowPrefs, EngineSession, FeatureAuto, EarGate)
 ↓
Domain (TextPostProcessor, LearnEngine, Cleanup, CourseCorrector, FieldPolicy)
 ↓
Data
 ├── Offline: Room FTS, FlowPrefs, system SpeechRecognizer
 ├── Online: ProviderRegistry → CloudHttp / AndroidCloudSocket / brains
 └── Secure: AndroidSecretStore (AES-GCM + AndroidKeyStore)
```

## Feature status

| Feature | Status | Evidence |
|---------|--------|----------|
| Bubble + a11y insert | Live | `bubble/FlowAccessibilityService.kt` |
| System STT continuous | Live | `stt/SttEngine.kt` |
| Cleanup / style / cmds | Live | `text/` |
| Dict + LearnEngine | Live (autoLearn + clearLearned) | `LearnEngine` / `DictationRepository` |
| Cloud brains HTTP | Live BYOK | `ai/providers/cloud/` |
| Cloud ears WS | Live (key required) | `AndroidCloudSocket` + `AndroidPcmMic` |
| on_phone / laptop / custom_stt | Stub (honest gate) | `EarGate` |
| Recorder / FGS / WorkManager | Absent | — |

## Proof

- Unit tests: **701** / 0 fail
- Report: [`docs/FULL_AUDIT_REPORT_2026-08-15.md`](FULL_AUDIT_REPORT_2026-08-15.md)
- Phone Files GO: still Mitun ☐
