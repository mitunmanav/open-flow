# Architecture inventory — 2026-08-15

**Lane:** `main` @ `050b3e4` · cloud WIP: `.worktrees/cloud-ear-ws` `feat/cloud-ear-ws` (uncommitted)

## Layer map

```text
UI (Compose MainActivity / shell / engine picker)
 ↓
Presentation (FlowPrefs, EngineSession, FeatureAuto)
 ↓
Domain (TextPostProcessor, LearnEngine, Cleanup, CourseCorrector, FieldPolicy)
 ↓
Data
 ├── Offline: Room FTS, FlowPrefs, system SpeechRecognizer
 ├── Online: ProviderRegistry → CloudHttp / CloudSocket / brains
 └── Secure: AndroidSecretStore (AES-GCM + AndroidKeyStore)
```

| Package | Role |
|---------|------|
| `ui/` | Compose screens, picker, privacy, walkthrough |
| `bubble/` | AccessibilityService + overlay insert |
| `stt/` | SpeechEngine, ContinuousPolicy, cloud/host/ondevice ears |
| `ai/` | TextAIProvider brains, HostUrl, CloudHttp |
| `text/` | Post-process, LearnEngine, FeatureGate |
| `data/` | Room repo, dict, sessions |
| `secrets/` | BYOK store |
| `notify/` | DictationNotifier |
| `engine/` | ProviderRegistry, EnginePrefs |
| `privacy/` | PrivacyDefaults, retention |

## Feature status

| Feature | Status | Evidence |
|---------|--------|----------|
| Bubble + a11y insert | Live | `bubble/FlowAccessibilityService.kt` |
| System STT continuous | Live | `stt/SttEngine.kt` `ContinuousPolicy.kt` |
| Cleanup / style / cmds | Live | `text/` |
| Dict + LearnEngine | Live (autoLearn pref) | `LearnEngine.kt` `FlowPrefs.autoLearn` |
| History search/export | Live | `data/` `export/` |
| Cloud brains HTTP | Live BYOK | `ai/providers/cloud/` |
| Cloud ears WS | Partial | **main=`FailSoftSocket`** · WIP=`AndroidCloudSocket`+PCM |
| on_phone ear/brain | Stub | `OnDeviceEar.kt` / identity brain |
| laptop ear | Stub/partial URL gate | `LaptopEar.kt` |
| custom_stt | Stub | picker only |
| Recorder | Absent | no package |
| FGS / WorkManager | Absent | none |

## Offline / online boundary holes

1. `main` wires `FailSoftSocket` — cloud ear pick = silent no-audio (picker on main still system-only via `earEnabled`).
2. WIP unlocks cloud ears in `EarGate` without surfacing WS `onFailure` to `SpeechEngine.Listener.onError`.
3. History `learnFromEdit` may ignore `autoLearn` (bubble path checks pref).

## Perf suspects

| Suspect | Why |
|---------|-----|
| Continuous STT restart | silence → recreate loop |
| A11y service always-on | overlay + field watch |
| WIP AudioRecord + OkHttp ping 20s | mic+WS while cloud listening |
| Eager registry | mitigated by `by lazy` in OpenFlowApp |

## Security surface

| Item | State |
|------|-------|
| INTERNET | Declared; unused until pick |
| allowBackup | false |
| Exported | MainActivity + FlowAccessibilityService |
| Secrets | AndroidSecretStore AES-GCM |
| NSC | cleartext blocked except localhost/LAN literals |
| Analytics/ads | None |

## Reuse vs refactor

- Reuse: LearnEngine, ProviderRegistry, CloudEar hierarchy, SecretStore, Room.
- Finish: cloud WS error path + merge WIP.
- Do not rebuild: bubble/a11y path, local polish pipeline.
