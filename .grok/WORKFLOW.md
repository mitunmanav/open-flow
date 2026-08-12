# Open Flow — workflow (Superpowers + android-cli)

## Law

1. **Superpowers** — all skills when they apply (plans, TDD, worktrees, debug, verify, review).
2. **android-cli** — device, docs, layout, install.
3. **Bypass (only one):** multi-agent **inside** a worktree is allowed (max 5, different files).  
   Do **not** treat Superpowers as forbidding worktree sub-agents.

## Active trees

Keep lean. After merge → remove worktree + delete branch.

| Path | Role |
|------|------|
| repo root `main` | Ship line only |

## Loop

```
superpowers check → plan → worktree → TDD → android verify → commit → merge → remove wt
```

## Product

Bubble+a11y · en-US · no cloud AI default · modern brutal UI · rules-based polish.
