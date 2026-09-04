# Open Flow

[![Release](https://img.shields.io/github/v/release/mitunmanav/open-flow)](https://github.com/mitunmanav/open-flow/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Site:** [mitunmanav.github.io/open-flow](https://mitunmanav.github.io/open-flow/)

> **Don’t type, just speak.** An open-source AI voice dictation app for Android that turns messy speech into clean, polished writing in any app.

---

## Why Open Flow?

Traditional mobile voice typing requires switching to a dedicated keyboard, struggles with filler words, and often sends your data into closed cloud silos.

**Open Flow is different:**
- **Keep your keyboard:** A subtle, draggable bubble floats over whatever app you are using (WhatsApp, Slack, Gmail, browser). Tap to speak, tap to finish.
- **Speaks your language:** Transforms rambling speech, hesitations, and stutters into structured, punctuated, clear sentences.
- **Privacy first:** No account required. Zero telemetry. No ads. `INTERNET is declared` only for optional cloud transcription and offline model downloads; default dictation runs directly via your phone's speech engine or on-device Whisper.
- **MIT Open Source:** Built in modern Kotlin, Jetpack Compose, and Whisper C++ integration.

---

## Before & After

| Raw Rambling Speech | Clean Polished Text |
|---------------------|---------------------|
| *"um hey team so i was thinking maybe we should move the sync to thursday because uh wednesday looks completely slammed for john and sarah anyway let me know what works"* | *"Hey team, let's reschedule our sync to Thursday — Wednesday is packed for John and Sarah. Let me know if that works for you."* |
| *"bullet points buy eggs milk bread and oh yeah also paper towels"* | • Eggs<br>• Milk<br>• Bread<br>• Paper towels |

---

## Key Features

1. **Floating Bubble Dictation**
   - Floats over any application without replacing your favorite keyboard (Gboard, SwiftKey, Samsung).
   - Drag to dock anywhere along the screen edges.
   - Quick copy chip appears after dictation for 10 seconds.

2. **Spoken Edits & Formatting**
   - Natural voice commands: say *"strike that"* to erase the last sentence, *"new paragraph"* to break, or *"capitalize that"*.
   - Automatic number formatting and list detection.

3. **Searchable Local History**
   - Every dictation is saved securely on your device.
   - Search, copy, share, or export your history to JSON.
   - Configurable retention: keep indefinitely, auto-clear after 24 hours, or never save.

4. **Multi-Language Support**
   - 40+ languages supported out of the box with quick language switching directly from the bubble badge.

---

## Quickstart & Installation

1. **Download APK:** Grab the latest release APK from **[Releases](https://github.com/mitunmanav/open-flow/releases/latest)**.
2. **Install & Allow:**
   - Open the downloaded APK in your Files app (Play Protect prompt is normal for sideloaded apps).
   - Grant **Microphone** and **Accessibility Service** permissions.
   - *If the Accessibility toggle is greyed out:* Go to **App info → ⋮ (top right) → Allow restricted settings**.
3. **Start Dictating:** Tap any text box in any app, tap the floating bubble, speak your thought, and tap again to insert.

Detailed documentation:
- [Installation Guide](docs/INSTALL.md)
- [User Guide](docs/GUIDE.md)
- [Privacy Policy](docs/PRIVACY.md)
- [Feature Comparison vs Others](docs/COMPARISON.md)

---

## Honest Speech Privacy

- By default, Open Flow uses your phone’s system speech recognizer (on many Android devices, this is Google).
- Audio processing follows your device recognizer settings.
- For complete offline privacy where audio never leaves your device, enable **Whisper on phone** in Open Flow Settings.

---

## Dev vs Launch

| Track | Target | Tools & Path | Verification |
|-------|--------|--------------|--------------|
| **Dev** | Local development | Windows AVD `of_win` (`-gpu host`), wrap-adb | `./gradlew :app:assembleDebug` · `scripts/qa/gate.sh --quick` |
| **Dev Verify** | Quality gates | Unit tests, Lint, Layout inspection | `./gradlew :app:testDebugUnitTest :app:lintDebug` · `scripts/qa/visual-capture.sh` · `scripts/qa/functional-check.sh` |
| **Launch (GitHub)** | Automated Releases | GitHub Actions CI & release pipeline | `.github/workflows/ci.yml` · `release.yml` · `pages.yml` |
| **Launch (Play)** | Store Readiness | Signed AAB, Data Safety, 17 policy checks | `scripts/qa/play-check.sh` · `docs/store/` · `docs/specs/play-store-readiness.md` |

Author **Mitun only**. No `Co-Authored-By`. Push / tag only on explicit GO.

Device loop detail: [docs/testing.md](docs/testing.md).

---

## Building from Source

Prerequisites: Android SDK 36, NDK 28.2.13676358, JDK 17+.

```bash
# Run unit tests and lint
./gradlew :app:testDebugUnitTest :app:lintDebug

# Assemble debug APK
./gradlew :app:assembleDebug
```

---

## Community & Support

- **Questions & Ideas:** [GitHub Discussions](https://github.com/mitunmanav/open-flow/discussions)
- **Bug Reports:** [GitHub Issues](https://github.com/mitunmanav/open-flow/issues/new/choose)
- **Security Disclosures:** Report privately via the repo [Security](https://github.com/mitunmanav/open-flow/security) tab.

---

## License

Open Flow is licensed under the [MIT License](LICENSE) · [SECURITY.md](SECURITY.md).
