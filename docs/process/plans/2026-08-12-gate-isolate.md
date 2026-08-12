# Gate — worktree isolation (this repo only)

## Goal
- Open-flow gate stays **this project only**.
- Worktree A must not touch worktree B or main.
- Main must not touch `.worktrees/*`.
- Subagents in a tree stay in that tree (`cwd` required).
- No Grok `isolation=worktree` (extra trees mess us up).

## Files
- `.grok/hooks/bin/pretool_gate.py` — path jail + spawn cwd + no extra isolation
- `.grok/hooks/dev-gate.json` — also match `run_terminal_command`
- `.grok/hooks/bin/test_dev_gate.py`
- `.grok/rules/00-dev-gate.md` · FACTS · LESSONS · AGENTS · WORKFLOW

## TDD
1. RED: A writes B → deny
2. RED: A writes main `app/` → deny
3. RED: main writes `.worktrees/B` → deny
4. RED: spawn from A with no cwd → deny
5. GREEN
6. `python3 .grok/hooks/bin/test_dev_gate.py`

## Security
- Project hooks only. No `~/.grok/hooks`.
