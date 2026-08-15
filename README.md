# Open Flow

[![Release](https://img.shields.io/github/v/release/mitunmanav/open-flow)](https://github.com/mitunmanav/open-flow/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Site:** [mitunmanav.github.io/open-flow](https://mitunmanav.github.io/open-flow/)

Android app. A **floating bubble** types what you say. **Not a keyboard** — keep yours.

## What we are good at (the moat)

We do **not** win on speech accuracy. Others with Whisper/Vosk do.

We win on **habit + private history + trust**:

| Moat | What that means |
|------|------------------|
| Always there | Bubble over *your* keyboard. You do not switch IME. |
| Your archive | History, search, copy, share — on this phone. |
| Your words | Dictionary (one word) and snippets (a whole block). |
| Your rules | Keep / wipe 24h / never save. No account. |
| Inspectable | MIT. No ads. INTERNET declared; unused until you opt in. |

That is a product shell people can live in. It is **not** a better recognizer.

## Honest

Speech still uses the **system recognizer**. On many phones that is Google. Audio **can leave the device** — independent of our unused-by-default INTERNET permission.

Need that? Use FUTO / Sayboard / Whisper IME.

## Vs others (no sugar)

Full write-up + sources: [docs/COMPARISON.md](docs/COMPARISON.md).

| | Open Flow | Phone Whisper | FUTO Voice | Sayboard | Whisper IME | Kõnele | Wispr Flow |
|---|---|---|---|---|---|---|---|
| Keep your keyboard | **Yes** (bubble) | **Yes** (overlay) | No (IME / its keyboard) | No (voice keyboard) | No (IME) | No | Yes (bubble) |
| Speech engine | System SpeechRecognizer | Local model **or** OpenAI | Local Whisper | Local Vosk | Local Whisper | Kaldi **server** | Cloud AI |
| Audio can leave | **Yes, often** | Only if you pick cloud | No* | No* | No* | **Yes** on default server | **Yes** |
| INTERNET in app | **Declared; off until opt-in** | Models / optional API | Model download | Model download | Model download | Yes | Yes |
| Cleanup | Rule lists | Optional OpenAI | In-model | Weak | Weak | Server | Cloud LLM |
| Languages | **en-US only** | Several | Many | 20+ | Many | Depends | Many |
| License | MIT | Personal / permissive | **Not OSI** | GPL-3.0 | FOSS | Apache-2.0 | Closed |
| On F-Droid | **No** | No | Their repo | **Yes** | **Yes** | **Yes** | No |
| History + dict/snippets | **Yes** | Limited | Weak | No | No | No | Yes (their cloud) |
| Maturity | Early 0.1.x, debug-signed | Small project | Polished | Stable | Active | Older | Commercial |

\*After you download a local model.

**If you want…**

| Goal | Winner | Us |
|------|--------|-----|
| Voice never leaves the phone | FUTO, Sayboard, Whisper IME | We lose |
| Keep Gboard, bubble like Wispr | Phone Whisper or us | They transcribe better |
| Best wording | Wispr (closed) or Whisper apps | Rules, not AI |
| F-Droid today | Sayboard, Whisper IME, Kõnele | Not listed |
| MIT + history + dict/snippets + keep keyboard | Small club | **This is our niche** |

Wispr is closed. Kõnele’s **default** server can send audio unencrypted. FUTO is polished but not OSI FOSS.

## Install

1. APK from **[Releases](https://github.com/mitunmanav/open-flow/releases/latest)**.
2. Open in Files. Play Protect “unknown” is normal.
3. App → five screens → Accessibility → mic.
4. Grey switch: App info → **⋮ → Allow restricted settings**.
5. Text field → tap bubble → speak → **tap again**.

[Install](docs/INSTALL.md) · [Guide](docs/GUIDE.md) · [Launch checklist](docs/LAUNCH_CHECKLIST.md) · [Handoff](docs/HANDOFF.md)

Copy from **History**. No copy chip on the bubble.

## Talk (Discussions) vs report (Issues)

| Use | Where |
|-----|--------|
| Install help, “how do I…”, ideas | **[Discussions](https://github.com/mitunmanav/open-flow/discussions)** |
| Something is broken | **[Issues → Bug](https://github.com/mitunmanav/open-flow/issues/new/choose)** |
| Security hole | Repo **Security** tab — not public |

Start here: [Discussion #9](https://github.com/mitunmanav/open-flow/discussions/9).

Do not post names, emails, bank screens, or private dictation.

## Docs

[Website](https://mitunmanav.github.io/open-flow/) · [Architecture](https://mitunmanav.github.io/open-flow/architecture.html) · [Roadmap](https://mitunmanav.github.io/open-flow/roadmap.html) · [Privacy](docs/PRIVACY.md)

## Build

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Debug-signed sideload. Not Play / F-Droid.

MIT — [LICENSE](LICENSE) · [SECURITY.md](SECURITY.md)
