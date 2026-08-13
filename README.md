# Open Flow

[![Release](https://img.shields.io/github/v/release/mitunmanav/open-flow)](https://github.com/mitunmanav/open-flow/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Site:** [mitunmanav.github.io/open-flow](https://mitunmanav.github.io/open-flow/)

Android app. A **floating bubble** types what you say. **Not a keyboard** — keep yours.

## Honest

No INTERNET permission. We do not upload audio.

Speech still uses the **system recognizer**. On many phones that is Google. Audio **can leave the device**.

Need “voice never leaves this phone”? Use FUTO / Sayboard / Whisper IME. [Blunt comparison](docs/COMPARISON.md).

## Install

1. Get the APK from **[Releases](https://github.com/mitunmanav/open-flow/releases/latest)**.
2. Open it in Files. Play Protect “unknown” is normal.
3. Open the app → five short screens → Accessibility → mic.
4. If Accessibility is grey: App info → **⋮ → Allow restricted settings**.
5. Text field → tap bubble → speak → **tap again**.

[Install help](docs/INSTALL.md) · [How to use](docs/GUIDE.md)

Copy from **History** in the app. No copy chip on the bubble.

## Docs

| | |
|---|---|
| [Website](https://mitunmanav.github.io/open-flow/) | Home |
| [Architecture](https://mitunmanav.github.io/open-flow/architecture.html) | Diagrams |
| [Roadmap](https://mitunmanav.github.io/open-flow/roadmap.html) | Now / next / not planned |
| [Privacy](docs/PRIVACY.md) | What we touch |
| [How to report](docs/REPORT.md) | Bug or idea |

## Talk / report

- **[Discussions](https://github.com/mitunmanav/open-flow/discussions)** — questions, ideas
- **[Issues](https://github.com/mitunmanav/open-flow/issues/new/choose)** — Bug or Idea  
  What you tapped. What you expected. No personal data.  
  Security: repo **Security** tab, not a public issue.

## Build

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Debug-signed sideload. Not Play / F-Droid.

## License

MIT — [LICENSE](LICENSE) · [SECURITY.md](SECURITY.md)
