# Open Flow — workflow (Superpowers + android-cli)

## Gate (this repo only)

- Rules: `.grok/rules/00-dev-gate.md`
- Hooks: `.grok/hooks/dev-gate.json`
  - SessionStart / UserPromptSubmit: stamp + short inject
  - PreToolUse: deny `app/` writes on main (INTERNET perm not hook-blocked)
  - Stop / SubagentStop: block fake PASS (need real command / android-cli proof)
- Trust once: `/hooks-trust` in this folder (or trust path in `~/.grok/trusted_folders.toml`)
- Global Grok rules stay global; **this gate does not touch other projects.**

## Law

1. **Superpowers** — all skills when they apply (plans, TDD, worktrees, debug, verify, review).
2. **android-cli** — same weight. `android docs` / `layout` / `screen` / `install` / `info`. No API guesses.
3. **Web / internet** — search before feature work.
4. **Bypass (only one):** multi-agent **inside** a worktree is allowed (max 5, different files).  
   Do **not** treat Superpowers as forbidding worktree sub-agents.
5. **Caveman** — DID / PASS-FAIL / NEXT / ASK. No essays.
6. **Proof** — no PASS without test/build/android-cli output.

## Active trees

Keep lean. After merge → remove worktree + delete branch.

| Path | Role |
|------|------|
| repo root `main` | Ship line only |

## Loop

```
superpowers + android-cli + web → plan → worktree → TDD → android verify → commit → merge → remove wt
```

## Product

Bubble+a11y · en-US · no cloud AI default · modern brutal UI · rules-based polish.
