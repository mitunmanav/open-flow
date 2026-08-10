# Open Flow — session handoff (pickup)

**Updated:** 2026-08-10  
**Repo:** `/home/mitun/open-flow`  
**Branch:** `main`  
**Active worktree:** repo root  
**Full history:** `docs/BASELINE.md`  
**Plan just shipped:** `docs/superpowers/plans/2026-08-10-14-polish.md`

---

## Product (locked)

Flow Bubble + AccessibilityService, **NOT IME**. Local STT. MIT. No INTERNET default.

---

## Last done

**F14 polish** (web-guided Android):

- Bank/auth **package hide** (`PackagePolicy`)
- Bubble modes **full / compact / dot**
- **Shake** unsnooze (~2.7G)
- Listen **pulse**
- Tests **32** green · APK rebuilt

---

## NEXT

| ID | Work |
|----|------|
| F15 | Export/share history |
| F16 | Memo recorder |
| F17 | Language pack UI |

---

## Rebuild

```bash
export JAVA_HOME=$HOME/.local/jdk ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$PATH"
cd /home/mitun/open-flow
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest :app:assembleDebug --offline
```

Phone: Settings → Compact/Dot · snooze+shake · bank app hide · dictate pulse.

---

## Paste

```
Continue open-flow. AGENTS + docs/BASELINE.md + HANDOFF.
F14 done. NEXT F15 export or F16 recorder.
```
