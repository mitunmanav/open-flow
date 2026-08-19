# Changelog

## Unreleased

## 0.1.7 — 2026-08-18

GitHub sideload. versionCode 8 · versionName 0.1.7. First public APK since **0.1.5** (0.1.6 was never published).

### Added
- Bubble look: colors, shape, size, opacity, custom icon, per-action haptics.
- Bubble stays while the keyboard is up. Quick Settings tile. Shows in the app you are using.
- Speech languages in Settings (English US/UK/IN/AU/CA, Spanish, French, German, Hindi, Portuguese, Italian, Japanese, Chinese).
- Speak edits: strike that, X not Y, change that to, or rather, forget it, chained “actually / wait no”.
- Spoken lists, caps, quotes, punctuation aliases, question marks, run-on splits.
- App-aware cleanup (chat / email / work / docs). Local command mode (bullets, numbered list, case).
- Routing: local-only, local-then-AI, or AI-first.
- Learns a replacement after two matching edits. Protects close names (Mike vs Mike II).
- Hub: Home, Dictionary, Snippets, Style, Insights. Insights stay on the phone.
- History: search, copy, share, JSON export, save file, raw toggle. Audio memos follow keep / wipe rules.
- In-app Privacy Policy and Terms. Help opens GitHub Discussions.
- Dictionary A–Z / newest / oldest. Listen Done. Press Enter to send.
- Optional on-device SpeechRecognizer. Reduced motion.

### Changed
- Quieter Home. Compact history actions. Copy is in History, not on the bubble.
- Dropped Soft skin. Dead controls wired or hidden.
- Insert uses the cleanup level you picked (does not rewrite just because a brain is on).
- targetSdk 36.

### Fixed
- Cloud speech: wait until the socket is open before sending audio.
- Sarvam: real WAV bytes. Auto never picks the Whisper stub.
- Custom bubble colors actually apply. Keyboard no longer hides the bubble.

### Known limits
- Debug-signed sideload. Not Play / F-Droid.
- INTERNET is declared; unused until you pick a cloud path or a model download.
- Phone speech may still use Google or the OEM.
- Bank apps may still warn about Accessibility.

## 0.1.6 — 2026-08-16

Not published on GitHub. versionCode 7 · versionName 0.1.6.

### Added
- Bubble only shows in the active app, not everywhere.
- Light brutal skin. Reduced motion.
- Disabled speech paths say so instead of sitting silent.

### Changed
- Controls that did nothing are wired or hidden.
- Cloud ear hardening. targetSdk / compileSdk 36.

### Known limits (at that build)
- Language: en-US only.
- Phone speech engine. Debug-signed sideload. Not Play / F-Droid.

## 0.1.5 — 2026-08-13

GitHub sideload. First engine picker + BYOK keys on the phone.

### Added
- Speech + AI picker (phone, laptop/LAN, named clouds, custom).
- API keys in Android Keystore. Last-4 mask in the UI.
- Laptop / LAN ear + brain. Public HTTP blocked; loopback / LAN allowed.
- Cloud ears (OpenAI, Deepgram, AssemblyAI, Sarvam) fail soft if not live.
- Per-app style. Five walkthrough screens.
- Bubble shapes, color tints, idle shrink, waveform bars.
- Fast / Balanced / Accurate speech profiles. Dark mode. Adaptive refresh.

### Changed
- History: day groups, search, edit raw, share markdown, copy clean.
- INTERNET declared; unused until you pick a cloud path.

### Fixed
- Cancel discards (does not save). Overlay Cancel / Done hits work.
- 24h wipe runs on launch. Bubble hidden on password / PIN fields.

### Known limits (at that build)
- Language: en-US only.
- Phone speech engine. Debug-signed. Copy from History, not the bubble.

## 0.1.1 — 2026-08-13

Ship-day polish.

- Tap again while listening inserts (Cancel discards, Done saves).
- Overlay hit-test for Cancel/Done.
- 24h wipe on app launch.
- versionCode 2 · versionName 0.1.1.

## 0.1.0 — 2026-08-12

First public GitHub release.

- Floating bubble + Accessibility insert (not a keyboard).
- History, dictionary, snippets, cleanup, bubble appearance.
- Retention: keep / wipe 24h / never store.
- versionName 0.1.0 · minSdk 26 · debug-signed sideload.
