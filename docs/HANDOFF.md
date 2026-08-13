# Open Flow — handoff (live)

**main tip:** `1d1c6d9` F22 Wispr-feel · 0.1.2  
**Product:** Bubble + AccessibilityService · **not** IME · en-US · local FOSS · minimal brutal UI

## WHERE (2026-08-13)

| Layer | Status |
|-------|--------|
| Local no-AI pipeline | **on main** |
| F17–F21 | **on main** |
| F22 setup wizard + banners + day groups + search shrink | **on main** |
| Worktrees | `minimax` only |

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

APK: `app/build/outputs/apk/debug/app-debug.apk` · Desktop `Open-Flow/apk/open-flow-0.1.2-debug.apk`

## NEXT

1. Phone: Setup a11y → mic → skip battery → tap → speak → tap again
2. Tiny cleanup model = later opt-in only

## ASK

None. F22 on main.
