# Open Flow

[![Release](https://img.shields.io/github/v/release/mitunmanav/open-flow)](https://github.com/mitunmanav/open-flow/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Site:** [mitunmanav.github.io/open-flow](https://mitunmanav.github.io/open-flow/)

Android app. A **floating bubble** types what you say. **Not a keyboard** — keep yours.

No account. No ads. MIT. INTERNET is **declared** and unused until you pick a cloud path or a model download.

## Honest about speech

Speech still uses the **phone’s recognizer**. On many phones that is Google. Audio **can leave the device**. That is the phone, not an Open Flow server.

Need “voice never leaves this phone”? Pick **Whisper on phone** in Settings — or FUTO / Sayboard / Whisper IME.

## What’s new in 0.1.9

Since **0.1.8**:

- **Copy chip** after dictation (Wispr-style, 10 s).
- **Stale-service banner** when the toggle is on but the process died.
- **Overlay retry** on addView fail, then an honest notice.
- **Language badge** on the idle bubble.

On **main** (not tagged yet): local cleanup v2 — N-best pick, spoken numbers, invariant gate, 5 s polish timeout, whisper loop guard.

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
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Device loop (this laptop): [docs/testing.md](docs/testing.md) — `scripts/qa/gate.sh`

Debug-signed sideload. Not Play / F-Droid.

MIT — [LICENSE](LICENSE) · [SECURITY.md](SECURITY.md)
