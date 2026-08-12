# Gate — smart speed + tiny memory

## Goal
- Superpowers **except** spawn: max **5** agents per worktree, never same file.
- **Small fix:** no worktree. Still proof. Still caveman.
- **Large feature:** worktree → up to 5 agents → test → merge main.
- Fast must stay smart. No skip plan/TDD/android-cli on large work.
- Tiny memory: NOW (live) + FACTS + LESSONS.

## Files
- `.grok/hooks/bin/pretool_gate.py` — allow app/ on main; cap 5 spawns per tree
- `.grok/hooks/bin/prompt_gate.py` — inject NOW + small/large + memory
- `.grok/hooks/bin/test_dev_gate.py`
- `.grok/memory/README.md` · `FACTS.md` · `LESSONS.md`
- `.grok/rules/00-dev-gate.md` · AGENTS · PROCESS · WORKFLOW · NOW

## TDD
1. RED: app write on main → allow (today deny)
2. RED: 6th spawn in same tree → deny
3. RED: inject mentions small/large + memory
4. GREEN
5. `python3 .grok/hooks/bin/test_dev_gate.py`

## Security
- No APK INTERNET. Agent web OK.

## Mitun test
```bash
python3 .grok/hooks/bin/test_dev_gate.py
```
Reload `/hooks`.
