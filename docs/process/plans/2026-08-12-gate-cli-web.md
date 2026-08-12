# Gate — internet + android-cli = Superpowers peer

## Goal
- Agent must use **web search** (internet) and **android-cli** every Android turn — same weight as Superpowers.
- Stop blocking INTERNET in the *hook* (Mitun needs that path).
- **Do not** add `INTERNET` to the APK unless Mitun later says so.

## Files
- `.grok/hooks/bin/pretool_gate.py` — drop INTERNET deny
- `.grok/hooks/bin/prompt_gate.py` — inject Superpowers + android-cli + web
- `.grok/hooks/bin/stop_gate.py` — `android docs|layout|install|info|screen` count as proof
- `.grok/hooks/bin/test_dev_gate.py`
- `.grok/rules/00-dev-gate.md` · `.grok/WORKFLOW.md`
- `AGENTS.md` · `docs/PROCESS.md` · `.grok/NOW.md`

## TDD
1. RED: INTERNET in manifest from worktree → must **allow** (today deny)
2. RED: prompt inject must say `android-cli` + `web`
3. RED: `android layout` in report counts as proof
4. GREEN: implement
5. `python3 .grok/hooks/bin/test_dev_gate.py`

## Security
- App still **no INTERNET perm** (SECURITY.md unchanged).
- Hook no longer blocks a future opt-in feature from adding it.
- Still deny `app/` edits on main.

## Mitun test
```bash
python3 .grok/hooks/bin/test_dev_gate.py
android info
```
Device: none.
