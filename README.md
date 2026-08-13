# Open Flow

[![Release](https://img.shields.io/github/v/release/mitunmanav/open-flow)](https://github.com/mitunmanav/open-flow/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Android app: a **floating bubble** types what you say into the app you are already in.

**Not a keyboard.** Keep Gboard (or any keyboard).

## The honest line

The app has **no INTERNET permission** and does **not** upload audio itself.

Speech still goes through Android’s **system recognizer**. On many phones that is Google, and audio **can leave the device**. If you need “voice never leaves this phone,” use a local Whisper/Vosk app — not this one. [Comparison (no sugar)](docs/COMPARISON.md).

## Install

1. Download the APK from **[Releases](https://github.com/mitunmanav/open-flow/releases/latest)**.
2. Open it in Files. “Unknown app” / Play Protect is normal. Install anyway.
3. Open Open Flow. Read the five short screens.
4. Turn on **Accessibility**.
5. If the switch is **grey**: Apps → Open Flow → **⋮ → Allow restricted settings**, then try again.
6. Allow the **microphone**.
7. Focus a text field → tap the bubble → speak → **tap again**.

Step-by-step: [docs/INSTALL.md](docs/INSTALL.md).

Copy old text from **History** in the app. There is no copy button on the bubble.

## What you get

| | |
|---|---|
| Bubble | Tap, tap again, or hold. X throws away. Drag to move. Drag down to hide. |
| Cleanup | None / Light / Medium / High — **rules on the phone**, not an AI model |
| Style | Formal / Casual / Very casual / Excited / Custom |
| History | Search, copy, share — stays on the phone |
| Dictionary | One word → another word |
| Snippet | Short trigger → a whole block of text |
| Retention | Keep / wipe after 24h / never save |

We **hide** the bubble in many bank and wallet apps. Those apps may still warn. That is their screen.

English only. MIT. No account. No ads.

## Docs

| Doc | For |
|-----|-----|
| [Guide](docs/GUIDE.md) | How to use it |
| [Install](docs/INSTALL.md) | First install and grey Accessibility |
| [Privacy](docs/PRIVACY.md) | What we touch |
| [Architecture](docs/ARCHITECTURE.md) | How the code is shaped |
| [Comparison](docs/COMPARISON.md) | Us vs FUTO, Sayboard, Whisper IME, Phone Whisper, Kõnele, Wispr — blunt |
| [How to report](docs/REPORT.md) | Bug or idea, no personal data |

## Report a problem

**[Open an issue](https://github.com/mitunmanav/open-flow/issues/new/choose)** → Bug or Idea.

Write what you tapped and what you expected. Phone + Android version if you know.  
No names, emails, bank screens, or private dictation.  
Security hole: use the repo **Security** tab, not a public issue.

## Build from source

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

This GitHub build is **debug-signed** for sideload. Not a Play Store / F-Droid release.

## License

MIT — [LICENSE](LICENSE)

## Security defaults

[SECURITY.md](SECURITY.md) — no INTERNET in the APK, backup off, Accessibility is insert-only.
