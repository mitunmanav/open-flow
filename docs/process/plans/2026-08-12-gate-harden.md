# Gate harden — real teeth before more app work

## Goal
Project-local Grok gate actually blocks fake PASS and `app/` edits on `main`.
No more “PASS-FAIL: PASS” or “NEXT:” as proof.

## Files
- `.grok/hooks/bin/stop_gate.py` — success claim needs real command proof
- `.grok/hooks/bin/pretool_gate.py` — deny `app/` writes outside worktree; deny INTERNET perm
- `.grok/hooks/bin/prompt_gate.py` — inject short gate context
- `.grok/hooks/bin/test_dev_gate.py` — unittest for gates
- `.grok/hooks/dev-gate.json` — add PreToolUse + SubagentStop
- `.grok/rules/00-dev-gate.md` — short hard law
- `.grok/WORKFLOW.md` · `.grok/NOW.md` — point at new teeth

## TDD
1. RED: `PASS-FAIL: PASS` + `NEXT:` with no gradlew/unittest → must block (today allows)
2. GREEN: fix stop_gate proof + claim detect
3. RED: write `app/…` from main cwd → deny; from `.worktrees/` → allow
4. GREEN: pretool_gate
5. RED: AndroidManifest + `android.permission.INTERNET` → deny
6. GREEN: same script
7. Run: `python3 .grok/hooks/bin/test_dev_gate.py`

## Security
- No new Android perms
- Gate stays fail-open on crash
- INTERNET add in manifest denied by hook
- Project-local only (no `~/.grok` hooks)

## Mitun test
```bash
python3 .grok/hooks/bin/test_dev_gate.py
# echo '{"lastAssistantMessage":"PASS done"}' | python3 .grok/hooks/bin/stop_gate.py
# expect: {"decision":"block",...}
```
Device: none. Then `/hooks` → trust still on.
