# Open Flow — handoff (live)

**main tip:** `42d4241` merge F15–F17  
**Product:** Bubble + AccessibilityService · **not** IME · en-US · local FOSS · minimal brutal UI

## WHERE (2026-08-12)

| Layer | Status |
|-------|--------|
| Local no-AI pipeline | **on main** |
| Minimal brutal UI + bubble | **on main** — charcoal/cream, hard square, 2dp |
| Mic insert + theme + nav + STT Hz | **on main** (was F15 worktree) |
| Worktrees | prune `15-bubble-mic-fix` after this merge |

## Pipeline (no AI model)

```
STT (+ API33 formatting extras)
  → PhraseMap / VoiceCommands (spoken punct, layout, backspace)
  → Cleanup None | Light | Medium | High (rules)
  → Writing style (Formal/Casual/Very casual/Excited/Custom)
  → Dictionary + snippets
  → ACTION_SET_TEXT
```

## Dev law (strict)

1. Superpowers on (this repo only). Only skip: no-subagents-in-worktree. Max 5/tree.
1b. Tree jail: A ≠ B ≠ main. Spawn cwd = that tree.
2. Small fix: no worktree. Large: plan + worktree + ≤5 agents + merge.
3. android-cli + agent web. Not APK INTERNET.
4. Memory: `.grok/NOW.md` + `.grok/memory/FACTS.md` + `LESSONS.md`
5. Caveman everywhere.

## Phone check

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Enable a11y → bubble → speak or debug inject
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (17M, built after merge)

## NEXT

1. USB Allow on phone (`unauthorized` now)
2. Install + smoke: hard square bubble, cream stroke, no purple
3. Spoken `period` / `backspace`
4. Then F16 recorder **or** export polish — Mitun pick

## ASK

None — smoke first.
