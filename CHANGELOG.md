# Changelog

## 0.1.5 — 2026-08-13

GitHub sideload ship.

- No copy chip on the bubble (copy from History)
- First-open walkthrough
- Brutal nav, pill overlay matches settings
- Back from Dictionary goes to Home
- Hide bubble on more bank/wallet apps
- Color presets + haptic Off/Light/Full
- Own cream/charcoal mark



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
- No INTERNET permission
- Honest STT network disclosure
- Retention: keep / wipe 24h / never store (enforced)
- Cloud + device-transfer backup excludes

### Build
- versionName `0.1.0` · minSdk 26 · targetSdk 35
- Debug-signed release for FOSS sideload
