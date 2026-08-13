# Open Flow

FOSS Android dictation. A **floating bubble** types what you say into any app.

**Not a keyboard.** Keep Gboard (or whatever you use).

- No account. No ads. No trackers.
- The app has **no INTERNET permission**.
- English only.
- MIT license.

**Honest about speech:** the phone’s own speech engine may still use Google. Open Flow does not upload audio.

Current sideload: **0.1.5**

---

## Install

1. Download the APK from [Releases](https://github.com/mitunmanav/open-flow/releases).
2. Open it in **Files**. Play Protect may say “unknown app” — that is normal. Install anyway.
3. Open **Open Flow**. Walk through the five screens.
4. Turn on **Accessibility** (the Flow Bubble).
5. If the switch is **grey**:
   - Settings → Apps → Open Flow → **⋮ → Allow restricted settings**
   - Then try Accessibility again.
6. Allow the **microphone**.
7. Battery settings are optional. Skip is OK.
8. Open any text field (not a password or bank app) → tap the bubble → speak → **tap again**.

Full steps: [docs/INSTALL.md](docs/INSTALL.md)

Copy text from **History** inside the app. There is no copy button on the bubble.

---

## What you get

| | |
|---|---|
| Bubble | Tap to talk. Tap again to insert. Hold to talk. X throws away. |
| Cleanup | None / Light / Medium / High (rules on the phone, not cloud AI) |
| Style | Formal / Casual / Very casual / Excited / Custom |
| History | Search, copy, share |
| Dictionary | Change one word |
| Snippet | Say a short trigger, paste a whole block |
| Privacy | Keep history / wipe after 24h / never save |

We **hide** the bubble in bank and wallet apps. Those apps may still warn that Accessibility is on. We cannot turn their warning off.

---

## Privacy

See [docs/PRIVACY.md](docs/PRIVACY.md).

---

## Build from source

```bash
export JAVA_HOME=${JAVA_HOME:-$HOME/.local/jdk}
export ANDROID_HOME=${ANDROID_HOME:-$HOME/Android/Sdk}
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

This first GitHub ship uses a **debug-signed** APK (sideload). Not for Play Store.

---

## License

MIT — [LICENSE](LICENSE)

## Security

[SECURITY.md](SECURITY.md) — no INTERNET in the APK, backup off, insert-only Accessibility.
