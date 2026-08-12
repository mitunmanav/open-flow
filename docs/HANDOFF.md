# Open Flow — handoff (live)

**main tip:** `e92c15d` F18 IME park  
**Product:** Bubble + AccessibilityService · **not** IME · en-US · local FOSS · minimal brutal UI

## WHERE (2026-08-12)

| Layer | Status |
|-------|--------|
| Local no-AI pipeline | **on main** |
| F17 chrome + F18 IME park | **on main** — orb above Gboard; listen bar 630×136 |
| Mic insert + theme + nav + STT Hz | **on main** |
| Worktrees | none |

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

1. F19 STT `BIASING_STRINGS` + `MASK_OFFENSIVE_WORDS=false`
2. Then leftover Wispr gaps (per-app style, copy chip)
3. Tiny cleanup model = later opt-in only

## ASK

None — F19 next unless Mitun says else.
