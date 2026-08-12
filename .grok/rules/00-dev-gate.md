# Open Flow — Grok Build gate (this repo only)

Hard. Every turn. No skip.

## Voice
- Caveman ultra. Short lines. YES/NO. DID / PASS-FAIL / NEXT / ASK.
- No essays. No generic agent advice.

## Required stack (same weight)
1. **Superpowers** — read `using-superpowers` first. All applicable skills.
2. **android-cli** — same as Superpowers. Read the skill. Use `android docs` / `layout` / `screen` / `install` / `run` / `info`. Do **not** guess Android APIs.
3. **Internet / web search** — required before feature work (current Android + security docs).
4. Code ship → TDD + worktree + plan + verify.
5. Only Superpowers bypass: multi-agent (max 5, never same file), worktrees OK.

## App code
- Feature work in `.worktrees/<id>-<slug>` on `feat/<id>-<slug>`.
- Do **not** edit `app/` on `main`. PreToolUse denies it.
- Plan first: `docs/process/plans/YYYY-MM-DD-<id>-<slug>.md`

## Loop (feature work)
```
web search + android docs → plan file → worktree → TDD → security → commit → report
```

## Claim gate
- Never claim PASS / fixed / done without **tool proof in the same message**.
- Proof = `gradlew` / `BUILD SUCCESSFUL` / `Ran N tests` / `adb install` / `android docs|layout|install|info|screen`.
- Writing `PASS-FAIL: PASS` or `NEXT:` is **not** proof.
- Author: Mitun only. No Co-Authored-By. No agent footers.

## Security
- Base app still has **no INTERNET perm** until a feature plan adds it.
- Hook does **not** block adding INTERNET in a worktree.
- No new perm without that feature’s plan.

## Boss files
- `AGENTS.md` · `docs/PROCESS.md` · `.grok/WORKFLOW.md` · `.grok/NOW.md`
