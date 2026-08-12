# LESSONS (append-only)

- `PASS-FAIL: PASS` is not proof. Need real command output.
- Hook deny on INTERNET ≠ “agent cannot search the web”.
- Do not leave merged worktrees sitting (disk + confusion).
- Bundled `general-purpose` asked for a long writeup — we shadow it. Caveman wins.
- Blocking all `app/` edits on main made tiny fixes slow. Small fix = no worktree.
- Fast without TDD/android-cli is dumb. Speed after the smart checks.
- Grok session often starts on main. Spawn **must** set `cwd` to the feature worktree or agents edit main.
- Grok `isolation=worktree` makes extra trees. Do not use. Use `.worktrees/` only.
