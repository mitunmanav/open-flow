# Open Flow — session handoff (pickup)

**Updated:** 2026-08-10  
**Repo:** `/home/mitun/open-flow`  
**Active worktree:** `.worktrees/chore-ponytail-cut`  
**Branch:** `chore/ponytail-cut` (tip: ponytail cut + this handoff)  
**main:** not merged yet — merge cut first  

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

## Last done (ponytail cut)

On `chore/ponytail-cut` (`0867d3d` cut + handoff commits after):

- **One store:** dictations only (dropped sessions + FTS dual stack)
- **Deleted dead:** TranscriptSearch, TranscriptExporter, empty RecordingService, SttConfig, FocusResolver
- **Slim manifest:** only `RECORD_AUDIO` (no FGS/notif/wake until real recorder)
- **Deps cut:** navigation-compose, viewmodel-compose, security-crypto
- **One polish path** on insert (dict + snippets + style)
- **PrivacyDefaults** = static report string
- **PASS:** `:app:testDebugUnitTest` + `:app:assembleDebug`

**Not done:** merge to `main` (ask Mitun).

---

## What already works (testable)

- Flow Bubble overlay (a11y) → talk → insert into focused field  
- Continuous listen (auto-restart after OS silence)  
- Long-press push-to-talk, drag, snooze 10 min (drag to bottom)  
- Mic gate, focus re-resolve, soft-mute beeps  
- Post-process: filler strip, caps, questions, light lists  
- Dictionary / Snippets / Style / Settings (bubble size, opacity, language tag)  
- Bottom nav: Home · Dictionary · Snippets · Style · Settings  
- History + stats (words, sessions, streak) via **dictations** table  
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
cd /home/mitun/open-flow/.worktrees/chore-ponytail-cut   # or main after merge
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk \
  "/mnt/c/Users/Mitun Manav G Y/Desktop/open-flow-debug.apk"
```

**Phone test:** Accessibility ON for Open Flow Bubble + mic → focus field → tap/hold bubble → stop to save history.

---

## Env (this machine)

- JDK: `$HOME/.local/jdk` (Temurin 17)  
- SDK: `$HOME/Android/Sdk` (platform 35, build-tools)  
- Gradle wrapper OK after first download  
- Windows Desktop user: `Mitun Manav G Y`

---

## Git / layout

```
open-flow/
├── AGENTS.md
├── app/
├── docs/HANDOFF.md    # THIS FILE
├── docs/FEATURES.md
└── .worktrees/chore-ponytail-cut/   # ACTIVE until merge
```

- Author: **Mitun only**. No Co-Authored-By.  
- Ponytail **full** was on this session (lazy cuts OK).  

---

## Rules for next agent (hard)

1. Read `AGENTS.md` first.  
2. Superpowers: plan → worktree → TDD → security → commit per feature.  
3. Max **5** sub-agents; **no two edit same file**.  
4. Interruptions: do side request, **resume** main work.  
5. Caveman ultra with Mitun.  
6. **Not IME** — bubble only.  
7. No INTERNET by default.  
8. **Do not re-add** sessions dual-stack / empty RecordingService / export without a real feature wire.

---

## NEXT (ordered)

| ID | Work |
|----|------|
| **0** | **Merge `chore/ponytail-cut` → `main`** (if Mitun YES) |
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
ACTIVE: .worktrees/chore-ponytail-cut · branch chore/ponytail-cut
DID: ponytail cut — sessions/export/empty recorder/unused deps gone; dictations only. Tests+APK green.
NEXT: merge chore/ponytail-cut → main (ask Mitun), then F14 polish or F16 recorder.
Do not resurrect dual Session* stack or empty RecordingService without real feature wire.
Rebuild APK to Desktop when shipping.
```
