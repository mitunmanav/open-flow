# Open Flow — Grok Build gate (this repo only)

Hard. Every turn. No skip. Main **and** subagents.

## Voice (everywhere)
- Caveman ultra. Short lines. YES/NO. DID / PASS-FAIL / NEXT / ASK.
- No essays. No filler. Same voice in subagents, hooks, NOW.
- Every `spawn_subagent` prompt **must** start with CAVEMAN + DID/PASS-FAIL/NEXT. Hook denies if missing.

## Required stack (same weight)
1. **Superpowers** — `using-superpowers` first. All applicable skills.
2. **Planning** — `writing-plans` **before** feature code. Plan file on disk first. No proceed without it.
3. **android-cli** — same weight. `android docs` / `layout` / `screen` / `install` / `run` / `info`. No API guesses.
4. **Agent web search** — required before feature work. **Not** APK INTERNET.
5. Code ship → TDD + worktree + verify.
6. Only Superpowers bypass: multi-agent (max 5, never same file), worktrees OK.

## App code
- Feature work in `.worktrees/<id>-<slug>` on `feat/<id>-<slug>`.
- Do **not** edit `app/` on `main`. PreToolUse denies it.
- Plan first: `docs/process/plans/YYYY-MM-DD-<id>-<slug>.md`

## Loop
```
web search + android docs → writing-plans file → worktree → TDD → security → commit → report
```

## Claim gate
- Never claim PASS / fixed / done without **tool proof in the same message**.
- Proof = `gradlew` / `BUILD SUCCESSFUL` / `Ran N tests` / `adb install` / `android docs|layout|install|info|screen`.
- Writing `PASS-FAIL: PASS` or `NEXT:` is **not** proof.
- Author: Mitun only.

## Security
- **No INTERNET perm in the APK.** Agent may search the web. Different things.
- No new perm without that feature’s plan.

## Boss files
- `AGENTS.md` · `docs/PROCESS.md` · `.grok/WORKFLOW.md` · `.grok/NOW.md`
