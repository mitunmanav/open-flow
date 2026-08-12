# MiniMax lab worktree

## Goal
Long-lived exclusive lab at `.worktrees/minimax` (`sandbox/minimax`).
Different Grok model works only there. Same open-flow rules.
Never merge the lab wholesale. Cherry-pick good slices to a new `feat/` branch.

## Locked
| Item | Value |
|------|--------|
| Path | `.worktrees/minimax` |
| Branch | `sandbox/minimax` (keep; do not prune) |
| Start | `cd .worktrees/minimax && grok --sandbox workspace` |
| Model | `/model` or `grok -m` in that session. No global default change. |
| Grok trees | Forbidden (`isolation=worktree`, `~/.grok/worktrees/`) |
| Device | Lab APK overwrites `app.openflow.debug` |

## Files
- `.grok/hooks/bin/pretool_gate.py` — reserved slug `minimax`
- `.grok/hooks/bin/test_dev_gate.py` — spawn-into-lab denied
- `scripts/open-minimax-lab.sh`
- Docs: AGENTS, PROCESS, WORKFLOW, 00-dev-gate, NOW, FACTS
- Lab-only pin on sandbox: `.grok/rules/02-minimax-lab.md`

## TDD
1. RED: main spawn cwd=minimax → deny
2. RED: other tree spawn cwd=minimax → deny
3. GREEN: `LAB_SLUGS={"minimax"}` in `jail_spawn`
4. `python3 .grok/hooks/bin/test_dev_gate.py`

## Security
- No MiniMax keys in repo. Do not read `~/.config/minimax/api_key`.
- No APK INTERNET. Agent web still OK.
- No `applicationId` change (two a11y services).

## How Mitun tests
```bash
python3 .grok/hooks/bin/test_dev_gate.py
git worktree list
cd .worktrees/minimax && git branch --show-current   # sandbox/minimax
scripts/open-minimax-lab.sh -m <model-id>
```
Device install optional. Overwrites debug app.

## Merge later
1. Note good commits on `sandbox/minimax`
2. New feat worktree from `main`
3. Cherry-pick (skip lab pin commit)
4. Test → merge feat only
