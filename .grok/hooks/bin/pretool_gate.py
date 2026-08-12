#!/usr/bin/env python3
"""open-flow only — PreToolUse gate.

Deny app/ file writes on main (must use .worktrees/).
INTERNET perm is allowed in a worktree (app still ships without it
until a feature plan adds it). Fail open on errors.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path


def file_from_input(tool_input: object) -> str:
    if not isinstance(tool_input, dict):
        return ""
    return str(
        tool_input.get("file_path")
        or tool_input.get("path")
        or tool_input.get("target_file")
        or ""
    )


def in_worktree(cwd: str, path: str) -> bool:
    for raw in (cwd, path):
        if not raw:
            continue
        if ".worktrees" in Path(raw).parts:
            return True
    return False


def is_app_path(path: str) -> bool:
    if not path:
        return False
    parts = Path(path).parts
    return "app" in parts


def decide(data: dict) -> dict | None:
    tool_input = data.get("toolInput") or data.get("tool_input") or {}
    path = file_from_input(tool_input)
    cwd = str(
        data.get("cwd")
        or data.get("workspaceRoot")
        or ""
    )

    if is_app_path(path) and not in_worktree(cwd, path):
        return {
            "decision": "deny",
            "reason": (
                "open-flow PreToolUse: do not edit app/ on main. "
                "Use .worktrees/<id>-<slug> + feat/<id>-<slug>. "
                "See .grok/rules/00-dev-gate.md"
            ),
        }
    return None


def main() -> None:
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)

    verdict = decide(data)
    if not verdict:
        sys.exit(0)

    sys.stdout.write(json.dumps(verdict))
    sys.stdout.flush()
    sys.exit(0)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        sys.exit(0)
