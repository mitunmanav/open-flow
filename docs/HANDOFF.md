# Open Flow — handoff (live)

**main tip:** `68c313c` F20 Wispr gaps  
**dev:** `feat/21-ship-day` F21 ship-day polish · 0.1.1  
**Product:** Bubble + AccessibilityService · **not** IME · en-US · local FOSS · minimal brutal UI

## WHERE (2026-08-13)

| Layer | Status |
|-------|--------|
| Local no-AI pipeline | **on main** |
| F17 chrome + F18 IME park + F20 gaps | **on main** |
| F21 tap-again + launch purge + Home chips | **this branch** |
| Worktrees | `.worktrees/21-ship-day` |

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

APK: `app/build/outputs/apk/debug/app-debug.apk` · Desktop `Open-Flow/apk/open-flow-0.1.1-debug.apk`

## NEXT

1. Merge F21 to main after Mitun check
2. F19 STT `BIASING_STRINGS` + `MASK_OFFENSIVE_WORDS=false`
3. Tiny cleanup model = later opt-in only

## ASK

None — F21 on this branch. Do not merge until Mitun GO.
