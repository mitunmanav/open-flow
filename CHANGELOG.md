# Changelog

## Unreleased

### Added
- Wispr Android FOSS gaps: copy/undo/paste chips (~10s), local field context, dict/snippet file import, History Use raw.
- Spec: `docs/process/specs/2026-08-16-wispr-android-desktop-map.md`.

### Changed
- Bubble drag: touch slop + VelocityTracker fling snap; compact Copy/Undo chips; Dict import File/Paste row.
- In-place UI: History/Dict/Snippets LazyColumn+keys; denser chips; no extra graphics layers.

### Fixed
- Cloud STT: queue WebSocket sends until the socket is open (no dropped `session.update` / first audio).
- Insert no longer LLM-rewrites just because a brain is picked; uses Settings cleanup level.
- Language catalog unlocked (`en-IN`, `hi-IN`, …). Settings → Speech language.
- Sarvam: send real WAV bytes (not raw PCM labeled as WAV).
- System ear: default SpeechRecognizer (not on-device-first). Balanced STT uses quality punctuation.

## 0.1.7 — 2026-08-16

Text pipeline improvements and TDD fixes. Verified 757 PASS / 0 fail / 0 err / 0 skip unit tests. Main tip `152d663`. versionCode 8 · versionName 0.1.7.

### Added
- **LearnEngine enhancements** — Case transfer is strictly applied to close variants, keeping map casing for genuinely different words. Multi-word replacements and plurals/suffixes are now supported correctly.
- **CourseCorrector expansion** — Now resolves complex triple-chained corrections (e.g., "meet at 3 actually 4 wait no 5"), handles structural chunk replacements without losing context, and tracks new correction markers like "change that to", "or rather", and "forget it".
- **CleanupPipeline upgrades** — Fully removes stacked discourse openers, collapses repeated phrases instead of just single words, handles hedge spacing without double commas, and parses spoken lists correctly.

### Changed
- Unit tests 735 → **757**.

## 0.1.6 — 2026-08-16

GitHub sideload ship (channel unchanged). Verified 735 PASS / 0 fail / 0 err / 0 skip unit tests (106 XML files). APK `open-flow-0.1.6-debug.apk` (debug-signed) at `app/build/outputs/apk/debug/app-debug.apk`. Main tip `5c68ac2`. versionCode 7 · versionName 0.1.6.

### Added
- **ActivePackageTracker** — bubble only shows in the active app, not everywhere.
- **Light brutal skin** — visual skin control in appearance.
- **Reduced-motion support.**
- **Honesty row for disabled ears** — disabled ears say so instead of sitting silent.

### Changed
- **Dead / hide controls wired or hidden** — engine picker + home module editors no longer show controls that do nothing.
- **Cloud ear hardening** — `MainThreadHop` + `PcmResample`.
- **targetSdk / compileSdk 36.**
- Unit tests 683 → **735**.

### Known limits (call out in release notes)
- **Language: en-US only.**
- **STT: phone engine.** May still use Google / OEM. We do not upload. INTERNET unused by Open Flow until opt-in.
- **Debug-signed** sideload APK. Bank apps may still warn about Accessibility. Not Play / F-Droid yet.

## 0.1.5 — 2026-08-13

GitHub sideload ship. Verified 603 PASS / 0 fail / 0 err / 0 skip unit tests. APK ~17 MB at `app/build/outputs/apk/debug/app-debug.apk`. Main tip `3ce83f2`.

### Added
- **Engine picker** — Speech + AI screen. Pick from `system | on_phone | laptop | openai | deepgram | assemblyai | sarvam | custom_stt` (ear) and `none | on_phone | laptop | openai | grok | MiniMax | deepseek | gemini | mistral | together | fireworks | openrouter | sarvam | anthropic | custom` (brain).
- **BYOK** — API keys stored in `AndroidSecretStore` (AES-GCM wrapped by AndroidKeyStore). Last-4 mask in UI. `EngineSession.saveKey` writes every key id the current pick needs.
- **Laptop / LAN ear + brain** — `LaptopEar` + `LaptopBrain` over user URL. `HostUrl` blocks public HTTP, allows loopback / RFC1918 / link-local only.
- **Cloud ears** — `OpenAiRealtimeEar` / `DeepgramEar` / `AssemblyEar` / `SarvamEar`. `FailSoftSocket` keeps callers safe — connect returns dead session, no live audio yet, no crash.
- **HTTP brain rate limit** — `RateLimit.DEFAULT_PER_MINUTE = 30` per host. Denied = `IOException("rate limited")`, not silent drop.
- **`FieldContext` / `CommandChrome` / `BrainPick`** — `HIGH_AI` + `COMMAND` + `FIELD_CONTEXT` features light up from `FeatureAuto.of(ear, brain)`.
- **`AppStylePolicy`** — per-app style (personal/work/email/other) wired into polish path.
- **`FtsQueryTest`** — Room history search covers both raw and clean text.
- **`HostUrlTest`** / `LaptopBrainTest` / `LaptopEarTest` / `NamedCloudTest` / `CloudProvidersTest` / `AndroidCloudHttpTest` / `AnthropicBrainTest` / `OpenAiCompatBrainTest` — pick wire contract covered by JVM tests.
- **`PrivacyHonestyTest`** / **`SecurityGateTest`** / **`UiPathTest`** / **`WindowChromeTest`** — manifest + privacy + UI source sanity tests that grep real files.
- **`BubbleTapPolicy.cancelled`** param — gesture cancel returns `Action.NONE`.
- **`FieldPolicy.skipHints`** — only hint + contentDescription (never live body) used for sensitive skip.
- **5 walkthrough pages** — What / Talk / Dict vs Snippet / Privacy / Ready.
- **Brutal cream / charcoal / ink theme** as ship default. M3 skin opt-in (defined, no UI picker).
- **Idle shrink** after 5s. **Waveform bars** (4-cell RMS).
- **Bubble shapes** — pill / circle / square / dot. **Color tints** — charcoal / cream / ink / stone.
- **Engine + STT profiles** — Fast / Balanced / Accurate.
- **Adaptive refresh** preference (60 / 90 / 120 / 144 Hz).
- **Dark mode** — system / light / dark.

### Changed
- Bubble uses `app.currentEar()` each tap (not raw `SttEngine`).
- Polish path goes through `EngineSession` + `FeatureAuto` + `SendPolicy.forBrain`.
- `NetworkSecurityConfig` cleartext: `localhost` / `127.0.0.1` / `10.0.2.2` / `192.168.x.x` / `172.16-31.x.x` / link-local v6.
- `androidx.compose.material:material-icons-extended` brought in for Dict / Snippets / Style icons.
- README, GUIDE, INSTALL, COMPARISON, ARCHITECTURE: re-verified against actual code (e.g. real test count, real APK path, real permissions).
- HISTORY in `MainActivity` shows day groups (`HistoryDays.group` → Today / Yesterday / Earlier) + search + edit raw + share markdown + copy clean.

### Fixed
- Bubble cancel discards (not saves) — `BubbleTapPolicy.Action.STOP_DISCARD`.
- Overlay hit-test for Cancel / Done (parent `OnTouch` ate clicks) — hit-test on `ACTION_UP`.
- `wipe_24h` retention purges on launch, not only on next save.
- Bubble security: a11y overlay uses `TYPE_ACCESSIBILITY_OVERLAY`, no `SYSTEM_ALERT_WINDOW`.
- `BubbleTapPolicy.cancelled` short-circuits — gesture cancel no longer fires `Action.START`.
- `FieldPolicy.isSensitive` catches password input-type variations even when `isPassword` flag false.
- README claimed "no INTERNET permission" — INTERNET is declared (NSC-blocked by default). README reworded to "declared; off until opt-in".

### Security
- INTERNET declared, NSC cleartext LAN-only, `allowBackup=false`, `dataExtractionRules` excludes root/file/database/sharedpref/external.
- API keys AES-GCM wrapped by AndroidKeyStore (`openflow_secrets_aes` alias). Plain key never logged. Uninstall deletes.
- Room v4 — **no** `fallbackToDestructiveMigration()`. Schema mismatch throws. User history preserved.
- `Recycle AccessibilityNodeInfo` after every read (`@Suppress("DEPRECATION")`).
- No clipboard leak on every insert — only on no-field fallback.
- `Recorder` permission not declared. FGS not in manifest. Recorder is V2.

### Known limits (call out in release notes)
- **Language: en-US only.** Other BCP-47 tags are forced to en-US.
- **STT: phone engine.** May still use Google / OEM. We do not upload. INTERNET unused.
- **Debug-signed** sideload APK. Release keystore is a later step (F-Droid / Play).
- **No bubble copy chip** — copy from History.
- **Cloud WS audio** is not live yet (`FailSoftSocket`). HTTP brain calls work end-to-end.

### Phone test (Mitun, before shipping APK to public)
- Install from **Files**, not only ADB. Android 13+ Restricted Settings path required.
- Bubble appears in text field; mic grant → tap → speak → tap → insert.
- Password / PIN / bank field → bubble hidden or no insert.
- History search / copy / share works.

## 0.1.1 — 2026-08-13

Ship-day polish. No INTERNET. No F19.

- Tap again while listening inserts (Cancel discards, Done saves)
- Overlay hit-test for Cancel/Done (parent OnTouch ate clicks)
- Retention wipe-24h purges on app launch, not only on next save
- Home: ready copy stays tap-again; battery chip = "Battery settings"; Keys chip "Speech on bubble"
- versionCode 2 · versionName 0.1.1

## 0.1.0 — 2026-08-12

First public GitHub release.

### Dictation
- Floating Flow Bubble + Accessibility insert (not an IME)
- Wispr-style: partials on bubble only; polish once on stop
- Field prefix preserved across async polish (no wipe of existing text)
- STT listener cleared on stop; soft silence/no-match errors suppressed
- Offline-prefer SpeechRecognizer + continuous restart

### App UI
- Material 3 shell: Home · History · Dictionary · Settings
- Redesigned Home modules (setup, practice, cleanup, recent)
- History search, share, Markdown export
- Dictionary, snippets, cleanup levels, bubble appearance

### Privacy
- No INTERNET permission (changed: INTERNET declared but blocked by NSC)
- Honest STT network disclosure
- Retention: keep / wipe 24h / never store (enforced)
- Cloud + device-transfer backup excludes

### Build
- versionName `0.1.0` · minSdk 26 · targetSdk 35
- Debug-signed release for FOSS sideload