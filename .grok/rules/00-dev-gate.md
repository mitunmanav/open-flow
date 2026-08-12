# Open Flow — Grok Build gate (this repo only)

Hard. Every turn. No skip.

## Voice
- Caveman ultra. Short lines. YES/NO. DID / PASS-FAIL / NEXT / ASK.
- No essays. No generic agent advice.

## Before tools
1. Read Superpowers skill that applies (`using-superpowers` first).
2. Code ship → TDD + worktree + plan + verify skills.
3. Android / API unknown → `android` CLI + web search (real docs).
4. Only Superpowers bypass: multi-agent (max 5, never same file), worktrees OK.

## App code
- Feature work in `.worktrees/<id>-<slug>` on `feat/<id>-<slug>`.
- Do **not** edit `app/` on `main`. PreToolUse denies it.
- Plan first: `docs/process/plans/YYYY-MM-DD-<id>-<slug>.md`

## Loop (feature work)
```
web search → plan file → worktree → TDD → security → commit → report
```

## Claim gate
- Never claim PASS / fixed / done without **tool proof in the same message**.
- Proof = real output: `gradlew`, `BUILD SUCCESSFUL`, `Ran N tests`, `adb install`.
- Writing `PASS-FAIL: PASS` or `NEXT:` is **not** proof.
- Author: Mitun only. No Co-Authored-By. No agent footers.

## Security
- No `INTERNET` in base manifest. Hook denies adding it.
- No new perm without that feature’s plan.

## Boss files
- `AGENTS.md` · `docs/PROCESS.md` · `.grok/WORKFLOW.md` · `.grok/NOW.md`
