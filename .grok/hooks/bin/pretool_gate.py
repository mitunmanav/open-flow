#!/usr/bin/env python3
"""open-flow only — PreToolUse gate.

Caveman spawn. Max 5 agents per worktree.
Path jail: one worktree cannot touch another or main.
Main cannot write into .worktrees/.
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
    "Agent",
}
FILE_TOOLS = {
    "search_replace",
    "write",
    "Write",
    "Edit",
    "MultiEdit",
    "NotebookEdit",
}
SHELL_TOOLS = {
    "run_terminal_command",
    "Bash",
    "bash",
}

CAVEMAN_MARK = re.compile(r"(?i)\b(caveman|DID:|PASS-FAIL:)\b")
OTHER_WT = re.compile(r"\.worktrees/([^/\s\"']+)")
MAX_SPAWNS = 5
LAB_SLUGS = {"minimax"}


def file_from_input(tool_input: object) -> str:
    if not isinstance(tool_input, dict):
        return ""
    return str(
        tool_input.get("file_path")
        or tool_input.get("path")
        or tool_input.get("target_file")
        or ""
    )


def resolve_path(raw: str, cwd: str) -> Path:
    p = Path(raw)
    if not p.is_absolute():
        p = Path(cwd) / p
    try:
        return p.expanduser().resolve()
    except Exception:
        return p


def worktree_slug(path: str | Path) -> str | None:
    try:
        parts = Path(path).expanduser().resolve().parts
    except Exception:
        parts = Path(str(path)).parts
    if ".worktrees" not in parts:
        return None
    i = parts.index(".worktrees")
    if i + 1 < len(parts):
        return parts[i + 1]
    return None


def is_spawn(data: dict) -> bool:
    name = str(data.get("toolName") or data.get("tool_name") or "")
    return name in SPAWN_TOOLS


def is_file_tool(data: dict) -> bool:
    name = str(data.get("toolName") or data.get("tool_name") or "")
    return name in FILE_TOOLS


def is_shell_tool(data: dict) -> bool:
    name = str(data.get("toolName") or data.get("tool_name") or "")
    return name in SHELL_TOOLS


def spawn_lacks_caveman(data: dict, tool_input: object) -> bool:
    if not is_spawn(data):
        return False
    if not isinstance(tool_input, dict):
        return True
    prompt = str(tool_input.get("prompt") or "")
    return not bool(CAVEMAN_MARK.search(prompt))


def spawn_key(data: dict) -> str:
    cwd = str(data.get("cwd") or data.get("workspaceRoot") or "main")
    slug = worktree_slug(cwd) or "main"
    safe = re.sub(r"[^A-Za-z0-9._-]+", "_", slug)
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


def deny(reason: str) -> dict:
    return {"decision": "deny", "reason": reason}


def jail_file(cwd: str, file_path: str) -> dict | None:
    if not file_path:
        return None
    src = worktree_slug(cwd)
    dst = worktree_slug(resolve_path(file_path, cwd))
    if src and dst != src:
        return deny(
            "open-flow jail: this worktree cannot write main or another tree. "
            f"you={src} target={dst or 'main'}"
        )
    if not src and dst:
        return deny(
            "open-flow jail: main cannot write .worktrees/. "
            f"Stay on main or cd into {dst}."
        )
    return None


def jail_spawn(cwd: str, tool_input: object) -> dict | None:
    if not isinstance(tool_input, dict):
        return deny("open-flow jail: bad spawn input")
    if str(tool_input.get("isolation") or "") == "worktree":
        return deny(
            "open-flow jail: do not use spawn isolation=worktree. "
            "Use project .worktrees/ + cwd. Extra trees mess us up."
        )
    child = str(tool_input.get("cwd") or "")
    child_slug = worktree_slug(child) if child else None
    parent = worktree_slug(cwd)
    if child_slug in LAB_SLUGS and parent != child_slug:
        return deny(
            "open-flow jail: minimax lab is exclusive. "
            "Start grok inside .worktrees/minimax. "
            "Do not spawn into it from main or another tree."
        )
    if not parent:
        return None
    if not child:
        return deny(
            "open-flow jail: spawn from a worktree must set cwd to that tree."
        )
    if worktree_slug(child) != parent:
        return deny(
            f"open-flow jail: spawn cwd must stay in {parent}. "
            "Do not point at another tree."
        )
    return None


def jail_shell(cwd: str, tool_input: object) -> dict | None:
    if not isinstance(tool_input, dict):
        return None
    cmd = str(tool_input.get("command") or "")
    if not cmd:
        return None
    src = worktree_slug(cwd)
    for match in OTHER_WT.finditer(cmd):
        other = match.group(1)
        if src and other != src:
            return deny(
                f"open-flow jail: shell in {src} mentioned .worktrees/{other}."
            )
        if not src:
            if re.search(
                r"\bgit\s+worktree\s+(add|list|prune|remove)\b", cmd
            ):
                continue
            return deny(
                "open-flow jail: main shell must not touch .worktrees/ files."
            )
    return None


def decide(data: dict) -> dict | None:
    tool_input = data.get("toolInput") or data.get("tool_input") or {}
    cwd = str(data.get("cwd") or data.get("workspaceRoot") or "")

    if spawn_lacks_caveman(data, tool_input):
        return deny(
            "open-flow PreToolUse: subagent prompt must be caveman. "
            "Start with CAVEMAN + DID / PASS-FAIL / NEXT / ASK."
        )

    if is_spawn(data):
        hit = jail_spawn(cwd, tool_input)
        if hit:
            return hit
        n = bump_spawn_count(data)
        if n > MAX_SPAWNS:
            return deny(
                f"open-flow: max {MAX_SPAWNS} subagents per worktree. "
                "Never same file."
            )

    if is_file_tool(data):
        hit = jail_file(cwd, file_from_input(tool_input))
        if hit:
            return hit

    if is_shell_tool(data):
        hit = jail_shell(cwd, tool_input)
        if hit:
            return hit

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
