# FACTS (append-only)

## Product
- Open Flow = Android FOSS. Bubble + a11y insert. **Not** IME. STT only.
- Default local. No account. No ads. No APK INTERNET.
- Agent may web-search. That is not an app permission.

## Dev
- This gate is **open-flow only**. No other repos.
- Superpowers on. Only skip: “no subagents in a worktree”.
- Max **5** subagents per worktree. Never same file.
- Worktrees are jails. A must not touch B. Main must not touch `.worktrees/`.
- Small fix: no worktree. Large: worktree + plan + test + merge main.
- android-cli same weight as Superpowers.
- Author: Mitun only.
- MiniMax lab: `.worktrees/minimax` on `sandbox/minimax`. Exclusive. Long-lived. Never merge wholesale. Cherry-pick good slices to a new `feat/` branch.

## Device
- USB adb. Wrapper: `~/.local/bin/adb` (Windows adb.exe).
- Desktop drop: `C:\Users\Mitun Manav G Y\Desktop\Open-Flow\`
