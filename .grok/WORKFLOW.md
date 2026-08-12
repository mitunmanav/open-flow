# Open Flow — worktree + dev workflow

## Active trees (keep lean)

| Path | Branch | Purpose |
|------|--------|---------|
| `/home/mitun/open-flow` | `main` | Ship line. Merge only green features. |
| `.worktrees/ui-optimize` | `feat/ui-optimize` | Modern brutal UI optimize (active) |
| `.worktrees/wispr-research` | `research/wispr-flow-deep` | Research docs only — no product code |

Remove a worktree when its branch is **fully merged** to main and dirty=0:
```bash
git worktree remove .worktrees/<name>
git branch -d feat/<name>   # if merged
git worktree prune
```

## Rules

1. **One feature = one worktree = one branch.** Never share files across parallel agents.
2. **Max 5 subagents** per task. No two agents edit the same file.
3. **Main stays clean.** WIP lives in worktrees. Stash only short-lived.
4. **Do not force-stop a11y** during live bubble tests.
5. **Author Mitun only.** No Co-Authored-By.
6. **Verify before merge:** `./gradlew :app:testDebugUnitTest` + compile green.
7. **Research ≠ product.** Research stays in docs/research or research wt; don't block main ship.

## Feature loop

```
plan → worktree → TDD → security → commit on branch → verify → merge main → remove wt
```

## Product locks (do not reopen)

- Bubble + AccessibilityService (not IME)
- en-US only for now
- Local FOSS, no cloud AI by default
- Cleanup levels + phrase map + styles = **rules**, not LLM
- Default UI skin: **modern brutal** (`VisualSkin.BRUTAL`) when that lands

## Pipeline (local no-AI)

```
STT (+ formatting extras)
  → PhraseMap / VoiceCommands
  → Cleanup None|Light|Medium|High
  → Writing style
  → Dictionary + snippets
  → ACTION_SET_TEXT
```

## Shell / tools

- Prefer `rtk` when available
- Android: `android` CLI (layout, screen, install, docs)
- Voice: caveman ultra — DID / PASS-FAIL / NEXT / SUGGEST / ASK
