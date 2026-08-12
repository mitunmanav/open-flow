#!/usr/bin/env python3
"""Tests for open-flow Grok Build gates. Run from repo root or this dir."""
from __future__ import annotations

import json
import subprocess
import sys
import unittest
from pathlib import Path

BIN = Path(__file__).resolve().parent
STOP = BIN / "stop_gate.py"
PRETOOL = BIN / "pretool_gate.py"
PROMPT = BIN / "prompt_gate.py"


def run_hook(script: Path, payload: dict) -> tuple[int, str]:
    proc = subprocess.run(
        [sys.executable, str(script)],
        input=json.dumps(payload),
        text=True,
        capture_output=True,
        check=False,
    )
    return proc.returncode, proc.stdout.strip()


def decision(stdout: str) -> str:
    if not stdout:
        return "allow"
    try:
        data = json.loads(stdout)
    except json.JSONDecodeError:
        return "allow"
    return str(data.get("decision") or "allow")


class StopGateTest(unittest.TestCase):
    def test_bare_pass_done_blocks(self) -> None:
        code, out = run_hook(
            STOP,
            {"hookEventName": "Stop", "lastAssistantMessage": "PASS done"},
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "block")

    def test_pass_fail_pass_with_next_is_not_proof(self) -> None:
        msg = "DID: stuff\nPASS-FAIL: PASS\nNEXT: merge"
        code, out = run_hook(
            STOP,
            {"hookEventName": "Stop", "lastAssistantMessage": msg},
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "block")

    def test_pass_fail_na_allows(self) -> None:
        msg = "DID: read files\nPASS-FAIL: n/a\nNEXT: plan"
        code, out = run_hook(
            STOP,
            {"hookEventName": "Stop", "lastAssistantMessage": msg},
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")
        self.assertEqual(out, "")

    def test_real_gradlew_proof_allows(self) -> None:
        msg = (
            "DID: tests\nPASS-FAIL: PASS\n"
            "./gradlew :app:testDebugUnitTest\nBUILD SUCCESSFUL\nNEXT: merge"
        )
        code, out = run_hook(
            STOP,
            {"hookEventName": "Stop", "lastAssistantMessage": msg},
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")
        self.assertEqual(out, "")

    def test_android_cli_proof_allows(self) -> None:
        msg = (
            "DID: device layout\nPASS-FAIL: PASS\n"
            "android layout -p\nNEXT: merge"
        )
        code, out = run_hook(
            STOP,
            {"hookEventName": "Stop", "lastAssistantMessage": msg},
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")

    def test_unittest_proof_allows(self) -> None:
        msg = "DID: hook tests\nPASS-FAIL: PASS\npython3 test_dev_gate.py\nRan 12 tests\nOK"
        code, out = run_hook(
            STOP,
            {"hookEventName": "Stop", "lastAssistantMessage": msg},
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")

    def test_stop_hook_active_allows(self) -> None:
        code, out = run_hook(
            STOP,
            {
                "hookEventName": "Stop",
                "stopHookActive": True,
                "lastAssistantMessage": "PASS done",
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")

    def test_session_end_allows(self) -> None:
        code, out = run_hook(
            STOP,
            {
                "hookEventName": "Stop",
                "reason": "channel_closed",
                "lastAssistantMessage": "PASS done",
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")


class PretoolGateTest(unittest.TestCase):
    def test_app_write_on_main_denied(self) -> None:
        self.assertTrue(PRETOOL.is_file(), "pretool_gate.py missing")
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow",
                "workspaceRoot": "/home/mitun/open-flow",
                "toolName": "search_replace",
                "toolInput": {"file_path": "/home/mitun/open-flow/app/src/Foo.kt"},
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_app_write_in_worktree_allowed(self) -> None:
        self.assertTrue(PRETOOL.is_file(), "pretool_gate.py missing")
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/gate-harden",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/gate-harden",
                "toolName": "search_replace",
                "toolInput": {
                    "file_path": "/home/mitun/open-flow/.worktrees/gate-harden/app/src/Foo.kt"
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")

    def test_docs_write_on_main_allowed(self) -> None:
        self.assertTrue(PRETOOL.is_file(), "pretool_gate.py missing")
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow",
                "workspaceRoot": "/home/mitun/open-flow",
                "toolName": "write",
                "toolInput": {"file_path": "/home/mitun/open-flow/docs/PROCESS.md"},
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")

    def test_internet_perm_allowed_in_worktree(self) -> None:
        self.assertTrue(PRETOOL.is_file(), "pretool_gate.py missing")
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/gate-cli-web",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/gate-cli-web",
                "toolName": "search_replace",
                "toolInput": {
                    "file_path": (
                        "/home/mitun/open-flow/.worktrees/gate-cli-web"
                        "/app/src/main/AndroidManifest.xml"
                    ),
                    "new_string": '<uses-permission android:name="android.permission.INTERNET"/>',
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "allow")


class PromptGateTest(unittest.TestCase):
    def test_user_prompt_emits_context(self) -> None:
        code, out = run_hook(
            PROMPT,
            {
                "hookEventName": "UserPromptSubmit",
                "cwd": str(BIN.parents[2]),
            },
        )
        self.assertEqual(code, 0)
        data = json.loads(out)
        ctx = data["hookSpecificOutput"]["additionalContext"]
        self.assertIn("GATE", ctx)
        self.assertIn("worktree", ctx.lower())
        self.assertIn("android-cli", ctx.lower())
        self.assertIn("web", ctx.lower())


if __name__ == "__main__":
    unittest.main(verbosity=2)
