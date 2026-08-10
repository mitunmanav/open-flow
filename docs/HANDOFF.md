# Open Flow — session handoff (pickup)

**Updated:** 2026-08-10  
**Repo:** `/home/mitun/open-flow`  
**Branch:** `main`  
**Tip:** `c13c195` (+ this handoff commit)  
**Active worktree:** repo root only  
**Full history / baseline:** `docs/BASELINE.md` ← **read this for all features**

---

## Product (locked)

**Open Flow** = FOSS Android app, MIT.

1. **Wispr job** — floating **Flow Bubble** + **AccessibilityService**, NOT IME. Keep normal keyboard. **STT only.**
2. **NeoSapien job** — local dictation history / later memo recorder.

| Rule | Value |
|------|--------|
| Default | Fully local, no account, no ads, no analytics |
| Online | Opt-in later only |
| Offline STT | SpeechRecognizer on-device prefer |
| License | MIT |

Map: `docs/FEATURES.md` · Law: `AGENTS.md` · Process: `docs/PROCESS.md` · Security: `SECURITY.md`

---

## Baseline (proved this save)

| Check | Result |
|-------|--------|
| Unit tests | 25, 0 fail |
| assembleDebug | PASS offline ~8s |
| Dual-stack dead code | GONE |
| Worktrees | none (clean) |
| APK | rebuild → `dist/` or Desktop |

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$PATH"
cd /home/mitun/open-flow
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest :app:assembleDebug --offline
cp app/build/outputs/apk/debug/app-debug.apk \
  "/mnt/c/Users/Mitun Manav G Y/Desktop/open-flow-debug.apk"
```

**Phone:** mic + Accessibility Open Flow → focus field → bubble → speak → stop → history.

---

## Shipped (short)

Bubble continuous STT insert · PTT/drag/snooze/size/opacity · post-process · dict/snippets/style · history/stats · 5-tab UI · dark mode · local only.

**Detail:** `docs/BASELINE.md` §3–4.

---

## NEXT

| ID | Work |
|----|------|
| **F14** | Bubble shrink, bank denylist, shake unsnooze, waveform |
| **F15** | Export/share history |
| **F16** | Memo recorder (audio + transcript + FGS when real) |
| **F17** | Language pack UI |
| later | Whisper / sync |

---

## Hard rules for next agent

1. Read `AGENTS.md` + `docs/BASELINE.md` + this file.  
2. Superpowers per feature. Max 5 sub-agents. No shared-file parallel edit.  
3. Not IME. No INTERNET default.  
4. Do **not** re-add Session dual-stack / empty RecordingService / Robolectric prefs tests.  
5. New feature IDs → append `docs/BASELINE.md` when ship.  

---

## Paste

```
Continue open-flow /home/mitun/open-flow. Read AGENTS.md + docs/BASELINE.md + docs/HANDOFF.md.
Bubble+a11y NOT IME. main clean, tests+APK green offline.
NEXT: F14 polish or F16 recorder (ask Mitun).
```
