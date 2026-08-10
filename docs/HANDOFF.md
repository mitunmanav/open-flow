# Open Flow — session handoff (pickup)

**Updated:** 2026-08-10  
**Repo:** `/home/mitun/open-flow`  
**Branch:** `main`  
**Tip:** `ca0b001` (chore: clean repo layout) · product tip before clean: `eafe209` (Wispr parity)

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

## What already works (testable)

- Flow Bubble overlay (a11y) → talk → insert into focused field  
- Continuous listen (auto-restart after OS silence)  
- Long-press push-to-talk, drag, snooze 10 min (drag to bottom)  
- Mic gate, focus re-resolve, soft-mute beeps  
- Post-process: filler strip, caps, questions, light lists  
- Dictionary / Snippets / Style / Settings (bubble size, opacity, language tag)  
- Bottom nav: Home · Dictionary · Snippets · Style · Settings  
- History + stats (words, sessions, streak)  
- Copy last chunk to clipboard  
- Password/sensitive fields skipped  
- Unit tests + `assembleDebug` green  

**APK for Mitun:** rebuild then put on Desktop  
`C:\Users\Mitun Manav G Y\Desktop\open-flow-debug.apk`  
(or `dist/` locally — gitignored)

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
cd /home/mitun/open-flow
echo "sdk.dir=$ANDROID_HOME" > local.properties
/tmp/gradle-8.9/bin/gradle :app:assembleDebug   # or ./gradlew if wrapper ok
cp app/build/outputs/apk/debug/app-debug.apk \
  "/mnt/c/Users/Mitun Manav G Y/Desktop/open-flow-debug.apk"
```

**Phone test:** Accessibility ON for Open Flow Bubble + mic → focus field → tap/hold bubble → stop to save history.

---

## Env (this machine)

- JDK: `$HOME/.local/jdk` (Temurin 17)  
- SDK: `$HOME/Android/Sdk` (platform 35, build-tools)  
- Gradle: `/tmp/gradle-8.9/bin/gradle` used successfully  
- Windows Desktop user: `Mitun Manav G Y`

---

## Git / layout

```
open-flow/
├── AGENTS.md          # hard rules for agents
├── LICENSE SECURITY README
├── app/               # Android source
├── docs/
│   ├── HANDOFF.md     # THIS FILE
│   ├── FEATURES.md    # Wispr A–Z map
│   ├── PROCESS.md
│   └── superpowers/plans/
├── dist/              # APKs ignored
└── .worktrees/        # feature worktrees, gitignored
```

- Merged feature worktrees **removed** (clean).  
- Old feature branches **deleted**.  
- Only `main` left.  
- Author: **Mitun only**. No Co-Authored-By.

---

## Rules for next agent (hard)

1. Read `AGENTS.md` first.  
2. Superpowers: plan → worktree → TDD → security → commit per feature.  
3. Max **5** sub-agents; **no two edit same file**.  
4. Interruptions: do side request, **resume** main work.  
5. Caveman ultra with Mitun.  
6. **Not IME** — bubble only.  
7. No INTERNET by default.  

---

## NEXT (ordered)

| ID | Work |
|----|------|
| F14 | Polish: bubble shrink modes, bank-app denylist, shake unsnooze, better waveform |
| F15 | Export/share history (.txt/.md), optional notifications |
| F16 | Memo **recorder** (audio + transcript) — NeoSapien depth |
| F17 | Multi-language UI polish / offline pack hints |
| later | Opt-in sync, Whisper opt-in |

Roadmap also: `docs/superpowers/plans/2026-08-10-master-fix-roadmap.md`

---

## Known limits (honest)

- Android STT ends on short silence → we restart (chunks).  
- Cloud-quality polish (Wispr) needs local models later.  
- Dictionary/snippets apply on insert (async).  
- Snippet expand = whole utterance match.  
- Soft-mute may affect music volume briefly during STT start.

---

## Paste into new chat

```
Continue open-flow at /home/mitun/open-flow per AGENTS.md + docs/HANDOFF.md.
Product: Wispr-style Flow Bubble (NOT IME) + local memory, MIT, offline STT.
main @ ca0b001. Next: F14 polish or F16 recorder — ask Mitun.
Rebuild APK to Desktop when shipping.
```
