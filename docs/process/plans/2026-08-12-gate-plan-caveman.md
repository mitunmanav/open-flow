# Gate — Superpowers plan first + caveman on subagents

## Goal
- **Plan first:** Superpowers `writing-plans` before any feature code.
- **Caveman everywhere:** main + subagents. No essays.
- **Internet = agent web search.** Not APK INTERNET perm.

## Files
- `.grok/hooks/bin/pretool_gate.py` — deny `spawn_subagent` if prompt lacks caveman
- `.grok/hooks/dev-gate.json` — match spawn_subagent
- `.grok/hooks/bin/prompt_gate.py` — inject plan + caveman + agent-web (not APK)
- `.grok/hooks/bin/test_dev_gate.py`
- `.grok/agents/{general-purpose,explore,plan}.md` — shadow bundled; caveman report
- `.grok/personas/caveman.toml`
- `.grok/rules/00-dev-gate.md` · AGENTS.md · PROCESS.md · WORKFLOW.md

## TDD
1. RED: spawn prompt with no DID/PASS-FAIL/caveman → deny
2. RED: spawn prompt with caveman card → allow
3. RED: prompt inject must say plan + caveman
4. GREEN: implement
5. `python3 .grok/hooks/bin/test_dev_gate.py`

## Security
- No APK INTERNET.
- Agent may web-search.

## Mitun test
```bash
python3 .grok/hooks/bin/test_dev_gate.py
```
Reload `/hooks`. New session picks up project agents.
