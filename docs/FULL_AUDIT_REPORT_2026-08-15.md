# Engineering report — Full Audit Harden (2026-08-15)

## 1. Already present
Bubble + a11y insert, system STT, local polish (cleanup/style/dict/snippets/LearnEngine), Room FTS, BYOK SecretStore, cloud brain HTTP, privacy retention, notifications.

## 2. Fixed
- Cloud ear WS silent failure → `onError` to SpeechEngine
- FailSoft no longer wired in app (real OkHttp + PCM)
- History learn ignored `autoLearn` → gated
- Honesty line now prefixes Local/Online

## 3. Added
- `AndroidCloudSocket`, `AndroidPcmMic`, `PcmSource`, `EarGate`
- Provider protocol hooks (Deepgram/Assembly/OpenAI/Sarvam)
- `LearnEngine.clearAll` + Dict Clear all learned
- `DictationNotifier.cancelAll`
- Inventory / security / perf plan docs

## 4. Refactored
- `CloudSocket.connect` gains `onError` callback
- `CloudEar` pumps mic PCM; session open/close hooks

## 5. Removed
- Dead `FailSoftSocket` as production wire (class kept for tests)

## 6. Offline architecture
System ear + local TextPostProcessor + Room. Default path never opens WS.

## 7. Online providers
BYOK → ProviderRegistry → CloudHttp / CloudSocket. EarGate live = system + 4 clouds. Stubs gated.

## 8. Self-learning
LearnEngine pairs from edits; autoLearn toggle; Dictionary inspect/delete; clearLearned wipe. Not ML training.

## 9. Security
BYOK KeyStore; allowBackup=false; no hardcoded keys; NSC; no analytics. See `2026-08-15-security-audit-m3.md`.

## 10–12. Battery / RAM / Backend
No FGS/WM. Lazy DI. Mic/WS stop on ear stop. No app server (client providers only).

## 13. Database
Room v6 unchanged this pass; dictionary `deleteAll` for wipe.

## 14. UI/UX
Local/Online honesty prefix; Clear all learned on Dict tab.

## 15. Tests
**701** unit tests / 0 failures (XML sum). APK `app-debug.apk` ~17.9 MB.

## 16. Performance
Source audit only — see M5 notes. Device profile pending.

## 17. Security findings
No High/Critical open.

## 18. Known limitations
- Phone Files GO still Mitun ☐ (no device in this session)
- on_phone / laptop / custom_stt / recorder still stub or absent
- en-US lock; debug-signed ship

## 19. Future
Device Battery Historian; optional laptop ear; recorder V2; release keystore.
