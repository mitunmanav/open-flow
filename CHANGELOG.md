# Changelog

## Unreleased

## 0.1.8 — 2026-08-25

GitHub sideload. versionCode 9 · versionName 0.1.8.

### Added
- **Whisper on phone** — whisper.cpp tiny.en (q5_1, arm64) runs fully offline. Model catalog + download in Settings, chunked transcribe with join, preload cache, honest on-stop picker.
- Hinglish nudge — hi-IN / en-IN speakers get a one-tap switch to Sarvam Code-mix mode.
- Session latency instrumentation (`adb logcat -s OpenFlow.Latency`) — listen → ear ready → first partial → insert, per session.
- Play-policy battery dialog: plain-language justification before REQUEST_IGNORE_BATTERY_OPTIMIZATIONS; skip stays available.
- Device smoke tests: prefs roundtrip, Room + FTS roundtrip, app launch.
- GitHub Actions CI: unit tests + lint + debug build on every push.

### Changed
- Battery: adaptive bubble pulse — ~30x fewer idle CPU wake-ups; ticker slows when idle and shrinks to one-shot near boundaries.
- Memory: retry-audio capture capped at 8 MB heap instead of unbounded growth on long sessions.
- Fillers: stretched forms now stripped ("uhh", "mm-hmm", "ermm", "mhmm") alongside the old list.
- Cleanup tone is app-aware end to end: chat / email / work / docs prompt guidelines reach the AI brain, not just the local rules.
- Cloud ear failure says what happened and what happens next ("kept what was heard — next listen uses a working ear") instead of a raw error string.
- Accessibility service declared `isAccessibilityTool`; subscribes only to events it actually handles (content-changed spam dropped).
- Screens peeled out of MainActivity; grouped Settings hub; shared page chrome; sectioned legal screen.
- Text correctness: word-aware finals merge, question marks only on real questions, pin/phone skipped only as whole words, bank-app hide only on real package tokens.
- Deps: AGP 8.13, Gradle 8.13, AndroidX bumps; x86_64 emulator slice added to release ABI set.

### Fixed
- Mic permission state read at activity create, not during composition.
- produceState assigns value unconditionally (History search).
- All cloud brain/ear factories fail loud with the missing piece named instead of bare `!!`.
- Listen stops cleanly when a bank app hides the bubble mid-session.
- PCM start fails loud; blank sessions no longer persisted.

### Known limits
- Debug-signed sideload. Not Play / F-Droid.
- Phone speech may still use Google or the OEM recognizer — pick Whisper on phone or a cloud ear for a different path.
- Bank apps may still warn about Accessibility.

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
