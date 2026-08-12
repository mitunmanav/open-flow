#!/usr/bin/env python3
"""open-flow only — UserPromptSubmit / SessionStart.

Refresh session stamp under .grok/rules so project rules stay hot.
Always exit 0. Fail open.
"""
from __future__ import annotations

import json
import os
import sys
import time
from pathlib import Path


def repo_root(cwd: str) -> Path | None:
    p = Path(cwd).resolve()
    for cand in [p, *p.parents]:
        if (cand / "AGENTS.md").is_file() and (cand / "app").is_dir():
            if (cand / "AGENTS.md").read_text(encoding="utf-8", errors="replace")[:200].find("open-flow") >= 0 or (
                cand / ".grok" / "WORKFLOW.md"
            ).is_file():
                return cand
        if (cand / ".grok" / "WORKFLOW.md").is_file() and (cand / "app").is_dir():
            return cand
    return None


def main() -> None:
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)

    cwd = str(
        data.get("cwd")
        or data.get("workspaceRoot")
        or os.environ.get("GROK_WORKSPACE_ROOT")
        or os.getcwd()
    )
    root = repo_root(cwd)
    if not root:
        sys.exit(0)

    rules = root / ".grok" / "rules"
    rules.mkdir(parents=True, exist_ok=True)
    stamp = time.strftime("%Y-%m-%d %H:%M %Z")
    out = rules / "01-session-stamp.md"
    body = "\n".join(
        [
            "# Session stamp (auto — open-flow gate)",
            f"# refreshed: {stamp}",
            "",
            "GATE ON: Superpowers → android-cli + web when Android → caveman report.",
            "Claim PASS only with test/build proof. Worktree for features.",
            "Only bypass: multi-agent max 5, different files.",
            "See: `.grok/rules/00-dev-gate.md`",
            "",
        ]
    )
    try:
        out.write_text(body, encoding="utf-8")
    except Exception:
        pass
    sys.exit(0)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        sys.exit(0)
