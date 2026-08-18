# Open Flow

[![Release](https://img.shields.io/github/v/release/mitunmanav/open-flow)](https://github.com/mitunmanav/open-flow/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Site:** [mitunmanav.github.io/open-flow](https://mitunmanav.github.io/open-flow/)

Android app. A **floating bubble** types what you say. **Not a keyboard** — keep yours.

No account. No ads. MIT. INTERNET is **declared** and unused until you pick a cloud path or a model download.

## Honest about speech

Speech still uses the **phone’s recognizer**. On many phones that is Google. Audio **can leave the device**. That is the phone, not an Open Flow server.

Need “voice never leaves this phone”? Use FUTO / Sayboard / Whisper IME — not this app.

## What’s new in 0.1.7

Since the last GitHub build (**0.1.5**):

- **Your bubble** — colors, shape, your own icon, haptics. Stays up with the keyboard.
- **Speak the edit** — “strike that”, “X not Y”, lists, caps. Works on the phone.
- **Insights + export** — usage and JSON history stay on this device.
- **More languages** — Hindi, Indian English, and others in Settings. Tile in Quick Settings.

Full list: [CHANGELOG.md](CHANGELOG.md).

## Install

1. APK from **[Releases](https://github.com/mitunmanav/open-flow/releases/latest)**.
2. Open in Files. Play Protect “unknown” is normal.
3. App → five screens → Accessibility → mic.
4. Grey switch: App info → **⋮ → Allow restricted settings**.
5. Text field → tap bubble → speak → **tap again**.

[Install](docs/INSTALL.md) · [Guide](docs/GUIDE.md) · [Privacy](docs/PRIVACY.md) · [Compare](docs/COMPARISON.md)

Older text: **History** in the app.

## Talk vs report

| Use | Where |
|-----|--------|
| Install help, “how do I…”, ideas | **[Discussions](https://github.com/mitunmanav/open-flow/discussions)** |
| Something is broken | **[Issues → Bug](https://github.com/mitunmanav/open-flow/issues/new/choose)** |
| Security hole | Repo **Security** tab — not public |

Start here: [Discussion #9](https://github.com/mitunmanav/open-flow/discussions/9).

Do not post names, emails, bank screens, or private dictation.

## Vs others

Full write-up: [docs/COMPARISON.md](docs/COMPARISON.md). Our niche is MIT + keep your keyboard + history / dictionary / snippets on the phone.

## Build

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Debug-signed sideload. Not Play / F-Droid.

MIT — [LICENSE](LICENSE) · [SECURITY.md](SECURITY.md)
