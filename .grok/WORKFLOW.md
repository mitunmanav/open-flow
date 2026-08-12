# Open Flow — workflow

## Gate

- Rules: `.grok/rules/00-dev-gate.md`
- Memory: `.grok/NOW.md` + `.grok/memory/FACTS.md` + `LESSONS.md`
- Hooks: caveman spawn; max 5/tree; **tree jail**; stop needs real proof
- **This repo only.** No global hooks.

## Law

1. Superpowers on. **Only skip:** no-subagents-in-worktree.
2. Max **5** agents per worktree. Never same file.
3. Small fix: no worktree. Large: plan → worktree → ≤5 agents → test → merge.
4. Fast must stay smart. TDD + android-cli + proof stay on for large.
5. Agent web ≠ APK INTERNET.
6. Caveman everywhere.
7. MiniMax lab: `.worktrees/minimax` / `sandbox/minimax`. Exclusive. Claude via `ccr "Claude Code"`. Same `.grok` gate only. Never merge wholesale.

## Loop (large)

```
writing-plans → web + android docs → worktree → ≤5 agents → TDD → merge → prune
```

## Memory

- NOW = live next only
- FACTS = durable truth
- LESSONS = scars
- Native `memory_search` too
