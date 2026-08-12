#!/usr/bin/env python3
"""open-flow only — PreToolUse gate.

Deny app/ file writes on main (must use .worktrees/).
Deny spawn_subagent if prompt lacks caveman card.
Agent web search is OK. APK INTERNET is not this hook's job.
Fail open on errors.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

SPAWN_TOOLS = {
    "spawn_subagent",
    "Task",
    "task",
}

CAVEMAN_MARK = re.compile(r"(?i)\b(caveman|DID:|PASS-FAIL:)\b")


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


def spawn_lacks_caveman(data: dict, tool_input: object) -> bool:
    name = str(data.get("toolName") or data.get("tool_name") or "")
    if name not in SPAWN_TOOLS:
        return False
    if not isinstance(tool_input, dict):
        return True
    prompt = str(tool_input.get("prompt") or "")
    return not bool(CAVEMAN_MARK.search(prompt))


def decide(data: dict) -> dict | None:
    tool_input = data.get("toolInput") or data.get("tool_input") or {}
    path = file_from_input(tool_input)
    cwd = str(
        data.get("cwd")
        or data.get("workspaceRoot")
        or ""
    )

    if spawn_lacks_caveman(data, tool_input):
        return {
            "decision": "deny",
            "reason": (
                "open-flow PreToolUse: subagent prompt must be caveman. "
                "Start with CAVEMAN + DID / PASS-FAIL / NEXT / ASK. "
                "No essays. See .grok/rules/00-dev-gate.md"
            ),
        }

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
