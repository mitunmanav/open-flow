#!/usr/bin/env bash
set -euo pipefail
ROOT="/home/mitun/open-flow/.worktrees/minimax"
if [[ ! -d "$ROOT" ]]; then
  echo "FAIL: lab missing. From main: git worktree add .worktrees/minimax sandbox/minimax"
  exit 1
fi
cd "$ROOT"
exec grok --sandbox workspace "$@"
