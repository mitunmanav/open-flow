#!/usr/bin/env python3
"""open-flow only — PreToolUse gate.

Deny spawn_subagent if prompt lacks caveman card.
Cap 5 spawns per worktree (or main cwd).
Small app/ edits on main are allowed.
Fail open on errors.
"""
from __future__ import annotations

import json
import os
import re
import sys
from pathlib import Path

SPAWN_TOOLS = {
    "spawn_subagent",
    "Task",
    "task",
}

CAVEMAN_MARK = re.compile(r"(?i)\b(caveman|DID:|PASS-FAIL:)\b")
MAX_SPAWNS = 5


def file_from_input(tool_input: object) -> str:
    if not isinstance(tool_input, dict):
        return ""
    return str(
        tool_input.get("file_path")
        or tool_input.get("path")
        or tool_input.get("target_file")
        or ""
    )


def is_spawn(data: dict) -> bool:
    name = str(data.get("toolName") or data.get("tool_name") or "")
    return name in SPAWN_TOOLS


def spawn_lacks_caveman(data: dict, tool_input: object) -> bool:
    if not is_spawn(data):
        return False
    if not isinstance(tool_input, dict):
        return True
    prompt = str(tool_input.get("prompt") or "")
    return not bool(CAVEMAN_MARK.search(prompt))


def spawn_key(data: dict) -> str:
    cwd = str(data.get("cwd") or data.get("workspaceRoot") or "main")
    sess = str(data.get("sessionId") or data.get("session_id") or "nosess")
    safe = re.sub(r"[^A-Za-z0-9._-]+", "_", f"{sess}:{cwd}")
    return safe[:180]


def spawn_count_dir() -> Path:
    raw = os.environ.get("OPENFLOW_SPAWN_STATE")
    if raw:
        return Path(raw)
    return Path("/tmp/open-flow-spawns")


def bump_spawn_count(data: dict) -> int:
    d = spawn_count_dir()
    d.mkdir(parents=True, exist_ok=True)
    f = d / f"{spawn_key(data)}.txt"
    n = 0
    if f.is_file():
        try:
            n = int(f.read_text(encoding="utf-8").strip() or "0")
        except ValueError:
            n = 0
    n += 1
    f.write_text(str(n), encoding="utf-8")
    return n


def decide(data: dict) -> dict | None:
    tool_input = data.get("toolInput") or data.get("tool_input") or {}

    if spawn_lacks_caveman(data, tool_input):
        return {
            "decision": "deny",
            "reason": (
                "open-flow PreToolUse: subagent prompt must be caveman. "
                "Start with CAVEMAN + DID / PASS-FAIL / NEXT / ASK. "
                "See .grok/rules/00-dev-gate.md"
            ),
        }

    if is_spawn(data):
        n = bump_spawn_count(data)
        if n > MAX_SPAWNS:
            return {
                "decision": "deny",
                "reason": (
                    f"open-flow PreToolUse: max {MAX_SPAWNS} subagents "
                    "per worktree. Finish or merge first. "
                    "Never same file. See .grok/rules/00-dev-gate.md"
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
