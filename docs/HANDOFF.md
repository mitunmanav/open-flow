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

1. **Superpowers** plugin — all features (see `AGENTS.md` / `docs/PROCESS.md`)
2. **android-cli** for device/docs/layout/install
3. **Exception:** multi-agent inside a worktree is **allowed** (bypass any Superpowers guidance that forbids worktree sub-agents)

## Phone check

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Enable a11y → bubble → speak or debug inject
```

## NEXT

1. Device smoke: modern brutal + cleanup + spoken `period` / `backspace`
2. F16 memo recorder (if Mitun orders)
3. Live harness gates Bound + overlay always

## ASK

None — pick NEXT with GO.
