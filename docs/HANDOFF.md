# Open Flow — handoff (live)

**main tip:** check `git log -1 --oneline`  
**Product:** Bubble + AccessibilityService · **not** IME · en-US · local FOSS · modern brutal UI

## WHERE (2026-08-12)

| Layer | Status |
|-------|--------|
| Local no-AI pipeline | **on main** — PhraseMap, cleanup levels, styles, STT formatting |
| Modern brutal UI | **on main** — `VisualSkin.BRUTAL` default |
| Research pack | `docs/research/` (docs only) |
| Worktrees | **none** (merged / removed) |

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

1. Superpowers on. Only skip: no-subagents-in-worktree. Max 5/tree.
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

## NEXT

1. Device smoke: modern brutal + cleanup + spoken `period` / `backspace`
2. F17 minimal brutal bubble (plan on disk) or F16 if Mitun orders
3. Leftover worktree: `.worktrees/15-bubble-mic-fix` (already on main?) — prune if yes

## ASK

None — pick NEXT with GO.
