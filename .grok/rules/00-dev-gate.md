# Open Flow — Grok Build gate (**this repo only**)

Does **not** apply to other Mitun projects. No global `~/.grok` hooks.

Hard. Every turn. Main **and** subagents.

## Voice
- Caveman ultra. DID / PASS-FAIL / NEXT / ASK. No essays.
- Spawn prompt **must** include CAVEMAN + DID/PASS-FAIL/NEXT.

## Required stack (same weight)
1. Superpowers — all skills **except** “no worktree subagents”.
2. **Bypass:** max **5** subagents **per worktree**. Never same file.
3. android-cli — `android docs` / `layout` / `screen` / `install` / `info`.
4. Agent web search — **not** APK INTERNET.
5. TDD + proof on ship. Author Mitun only.

## Size (smart speed)
- **Small fix** (one file / typo / hook tweak): **no worktree**. Still proof. Still caveman.
- **Large feature:** plan file → worktree → up to 5 agents → test → **merge main**.
- Fast ≠ dumb. Do not skip plan / TDD / android-cli / proof on large work.

## Isolate (hard)
- Tree A must not write tree B or `main`.
- `main` must not write `.worktrees/*`.
- Spawn from a tree **must** set `cwd` to that same tree.
- Do **not** use spawn `isolation=worktree` (extra trees mess us up).
- Never same file in two agents.
- After merge: remove that worktree. Do not leave ghosts.

## Loop (large)
```
web + android docs → writing-plans → worktree → ≤5 agents → TDD → commit → merge
```

## Memory (tiny)
- Live: `.grok/NOW.md`
- Durable: `.grok/memory/FACTS.md` (truths) · `LESSONS.md` (do not repeat)
- Also: native Grok `memory_search` / `memory_get`
- Search memory before re-asking Mitun.

## Claim
- No PASS without real output (`gradlew` / `Ran N tests` / `android layout`…).
- `PASS-FAIL: PASS` is not proof.

## Security
- No INTERNET in the APK. Agent may search the web.

## Boss
- `AGENTS.md` · `docs/PROCESS.md` · `.grok/WORKFLOW.md` · `.grok/NOW.md`
