# Open Flow — session handoff (pickup)

**Updated:** 2026-08-10  
**Repo:** `/home/mitun/open-flow`  
**Branch:** `main`  
**Tip:** `37a4a1f` — F12 UI foundation + ponytail cut merged  
**Active worktree:** repo root (no feature WIP)

---

## Product (locked)

**Open Flow** = FOSS Android app, MIT.

1. **Wispr job (Android style)** — floating **Flow Bubble** + **AccessibilityService**, NOT a keyboard/IME. Keep user’s normal keyboard. **STT only** (not TTS).
2. **NeoSapien job** — local dictation history / memory (recorder next).

| Rule | Value |
|------|--------|
| Default | Fully local, no account, no ads, no analytics |
| Online | Opt-in later only |
| Offline STT | Android SpeechRecognizer on-device prefer |
| License | MIT |

**Vs Wispr:** Wispr is cloud + account. We are local-first.

Full A–Z map: `docs/FEATURES.md`  
Process: `AGENTS.md` + `docs/PROCESS.md` + `SECURITY.md`

---

## Last done

1. **Ponytail cut merged** — one dictations store; dead Session/FTS/export/empty recorder/SttConfig/FocusResolver gone; slim deps.
2. **F12 UI foundation** — theme, colors, Type, Motion, Open* components, dark mode (system/light/dark), MainActivity rewired.
3. **Agent mess fixed** — dual branches reunited; Robolectric dark-mode test replaced with pure JVM `PrefsStore` tests (no big jar download).

**PASS:** `:app:testDebugUnitTest` + `:app:assembleDebug` (2026-08-10)  
**APK:** `.worktrees/12-ux-foundation/app/build/outputs/apk/debug/app-debug.apk` (or rebuild on main)

---

## What already works (testable)

- Flow Bubble overlay (a11y) → talk → insert into focused field  
- Continuous listen (auto-restart after OS silence)  
- Long-press push-to-talk, drag, snooze 10 min  
- Mic gate, focus re-resolve, soft-mute beeps  
- Post-process: filler strip, caps, questions, light lists  
- Dictionary / Snippets / Style / Settings  
- Bottom nav: Home · Dictionary · Snippets · Style · Settings  
- Dark mode toggle in Settings  
- History + stats via **dictations** table  
- Copy last chunk to clipboard  
- Password/sensitive fields skipped  

**Phone test:** Accessibility ON for Open Flow Bubble + mic → focus field → tap/hold bubble → stop to save history.

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$PATH"
cd /home/mitun/open-flow
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug --offline
cp app/build/outputs/apk/debug/app-debug.apk \
  "/mnt/c/Users/Mitun Manav G Y/Desktop/open-flow-debug.apk"
```

---

## Env

- JDK: `$HOME/.local/jdk` (Temurin 17)  
- SDK: `$HOME/Android/Sdk` (platform 35)  
- **Do not** add Robolectric for unit tests unless Mitun says OK (downloads huge jars). Use `PrefsStore` / pure Kotlin.

---

## Rules for next agent (hard)

1. Read `AGENTS.md` first.  
2. Superpowers: plan → worktree → TDD → security → commit per feature.  
3. Max **5** sub-agents; **no two edit same file**.  
4. Caveman ultra with Mitun.  
5. **Not IME** — bubble only.  
6. No INTERNET by default.  
7. **Do not re-add** sessions dual-stack / empty RecordingService / export without a real feature wire.  
8. **Do not** re-add Robolectric for simple prefs tests.

---

## NEXT (ordered)

| ID | Work |
|----|------|
| F14 | Polish: bubble shrink modes, bank-app denylist, shake unsnooze, better waveform |
| F15 | Export/share history (.txt/.md) — rebuild exporter when wiring UI |
| F16 | Memo **recorder** (audio + transcript) — add FGS + service when real |
| F17 | Multi-language UI polish / offline pack hints |
| later | Opt-in sync, Whisper opt-in |

Roadmap: `docs/superpowers/plans/2026-08-10-master-fix-roadmap.md`

---

## Known limits (honest)

- Android STT ends on short silence → we restart (chunks).  
- Cloud-quality polish (Wispr) needs local models later.  
- Dictionary/snippets apply on insert (async).  
- Snippet expand = whole utterance match.  
- Soft-mute may affect music volume briefly during STT start.  
- Room DB version **3**, destructive migrate (dev OK).  

---

## Paste into new chat

```
Continue open-flow at /home/mitun/open-flow per AGENTS.md + docs/HANDOFF.md.
Product: Wispr-style Flow Bubble (NOT IME) + local memory, MIT, offline STT.
main tip: F12 UI + ponytail cut merged. Tests+APK green offline.
NEXT: F14 polish or F16 recorder (ask Mitun).
Do not resurrect dual Session* stack, empty RecordingService, or Robolectric for prefs.
```
