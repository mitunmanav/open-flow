#!/usr/bin/env python3
"""Tests for open-flow Grok Build gates. Run from repo root or this dir."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
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
    def test_app_write_on_main_allowed_small_fix(self) -> None:
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
        self.assertEqual(decision(out), "allow")

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

    def test_spawn_without_caveman_denied(self) -> None:
        self.assertTrue(PRETOOL.is_file(), "pretool_gate.py missing")
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/gate-plan-caveman",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/gate-plan-caveman",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": "Explore the bubble chrome and write a detailed report."
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_sixth_spawn_denied(self) -> None:
        self.assertTrue(PRETOOL.is_file(), "pretool_gate.py missing")
        with tempfile.TemporaryDirectory() as tmp:
            env = os.environ.copy()
            env["OPENFLOW_SPAWN_STATE"] = tmp
            payload = {
                "hookEventName": "pre_tool_use",
                "sessionId": "test-sess",
                "cwd": "/home/mitun/open-flow/.worktrees/gate-smart-mem",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/gate-smart-mem",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": "CAVEMAN. DID: x\nPASS-FAIL: n/a\nNEXT: y",
                    "cwd": "/home/mitun/open-flow/.worktrees/gate-smart-mem",
                },
            }
            last_out = ""
            for _ in range(5):
                proc = subprocess.run(
                    [sys.executable, str(PRETOOL)],
                    input=json.dumps(payload),
                    text=True,
                    capture_output=True,
                    check=False,
                    env=env,
                )
                self.assertEqual(proc.returncode, 0)
                self.assertEqual(decision(proc.stdout.strip()), "allow")
            proc = subprocess.run(
                [sys.executable, str(PRETOOL)],
                input=json.dumps(payload),
                text=True,
                capture_output=True,
                check=False,
                env=env,
            )
            last_out = proc.stdout.strip()
            self.assertEqual(proc.returncode, 0)
            self.assertEqual(decision(last_out), "deny")

    def test_spawn_with_caveman_allowed(self) -> None:
        self.assertTrue(PRETOOL.is_file(), "pretool_gate.py missing")
        with tempfile.TemporaryDirectory() as tmp:
            env = os.environ.copy()
            env["OPENFLOW_SPAWN_STATE"] = tmp
            payload = {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/gate-plan-caveman",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/gate-plan-caveman",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": (
                        "CAVEMAN. Short lines. DID / PASS-FAIL / NEXT / ASK.\n"
                        "Explore bubble chrome. No essays."
                    ),
                    "cwd": "/home/mitun/open-flow/.worktrees/gate-plan-caveman",
                },
            }
            proc = subprocess.run(
                [sys.executable, str(PRETOOL)],
                input=json.dumps(payload),
                text=True,
                capture_output=True,
                check=False,
                env=env,
            )
            self.assertEqual(proc.returncode, 0)
            self.assertEqual(decision(proc.stdout.strip()), "allow")

    def test_tree_a_cannot_write_tree_b(self) -> None:
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/tree-a",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/tree-a",
                "toolName": "search_replace",
                "toolInput": {
                    "file_path": "/home/mitun/open-flow/.worktrees/tree-b/app/Foo.kt"
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_tree_cannot_write_main_app(self) -> None:
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/tree-a",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/tree-a",
                "toolName": "write",
                "toolInput": {"file_path": "/home/mitun/open-flow/app/src/Foo.kt"},
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_main_cannot_write_worktree(self) -> None:
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow",
                "workspaceRoot": "/home/mitun/open-flow",
                "toolName": "write",
                "toolInput": {
                    "file_path": "/home/mitun/open-flow/.worktrees/tree-b/app/Foo.kt"
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_spawn_from_tree_requires_same_cwd(self) -> None:
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/tree-a",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/tree-a",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": "CAVEMAN. DID: x\nPASS-FAIL: n/a\nNEXT: y"
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_spawn_into_other_tree_denied(self) -> None:
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/tree-a",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/tree-a",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": "CAVEMAN. DID: x\nPASS-FAIL: n/a\nNEXT: y",
                    "cwd": "/home/mitun/open-flow/.worktrees/tree-b",
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_spawn_isolation_worktree_denied(self) -> None:
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/tree-a",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/tree-a",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": "CAVEMAN. DID: x\nPASS-FAIL: n/a\nNEXT: y",
                    "cwd": "/home/mitun/open-flow/.worktrees/tree-a",
                    "isolation": "worktree",
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_shell_other_tree_denied(self) -> None:
        code, out = run_hook(
            PRETOOL,
            {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/tree-a",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/tree-a",
                "toolName": "run_terminal_command",
                "toolInput": {
                    "command": "rm /home/mitun/open-flow/.worktrees/tree-b/app/Foo.kt"
                },
            },
        )
        self.assertEqual(code, 0)
        self.assertEqual(decision(out), "deny")

    def test_spawn_counts_per_tree_independent(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            env = os.environ.copy()
            env["OPENFLOW_SPAWN_STATE"] = tmp

            def spawn(tree: str) -> str:
                payload = {
                    "hookEventName": "pre_tool_use",
                    "sessionId": "sess-iso",
                    "cwd": f"/home/mitun/open-flow/.worktrees/{tree}",
                    "workspaceRoot": f"/home/mitun/open-flow/.worktrees/{tree}",
                    "toolName": "spawn_subagent",
                    "toolInput": {
                        "prompt": "CAVEMAN. DID: x\nPASS-FAIL: n/a\nNEXT: y",
                        "cwd": f"/home/mitun/open-flow/.worktrees/{tree}",
                    },
                }
                proc = subprocess.run(
                    [sys.executable, str(PRETOOL)],
                    input=json.dumps(payload),
                    text=True,
                    capture_output=True,
                    check=False,
                    env=env,
                )
                self.assertEqual(proc.returncode, 0)
                return decision(proc.stdout.strip())

            for _ in range(5):
                self.assertEqual(spawn("tree-a"), "allow")
            self.assertEqual(spawn("tree-a"), "deny")
            self.assertEqual(spawn("tree-b"), "allow")

    def test_main_cannot_spawn_into_minimax_lab(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            env = os.environ.copy()
            env["OPENFLOW_SPAWN_STATE"] = tmp
            payload = {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow",
                "workspaceRoot": "/home/mitun/open-flow",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": "CAVEMAN. DID: x\nPASS-FAIL: n/a\nNEXT: y",
                    "cwd": "/home/mitun/open-flow/.worktrees/minimax",
                },
            }
            proc = subprocess.run(
                [sys.executable, str(PRETOOL)],
                input=json.dumps(payload),
                text=True,
                capture_output=True,
                check=False,
                env=env,
            )
            self.assertEqual(proc.returncode, 0)
            self.assertEqual(decision(proc.stdout.strip()), "deny")
            self.assertIn("minimax", proc.stdout.lower())

    def test_minimax_lab_can_spawn_inside_itself(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            env = os.environ.copy()
            env["OPENFLOW_SPAWN_STATE"] = tmp
            payload = {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/minimax",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/minimax",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": "CAVEMAN. DID: x\nPASS-FAIL: n/a\nNEXT: y",
                    "cwd": "/home/mitun/open-flow/.worktrees/minimax",
                },
            }
            proc = subprocess.run(
                [sys.executable, str(PRETOOL)],
                input=json.dumps(payload),
                text=True,
                capture_output=True,
                check=False,
                env=env,
            )
            self.assertEqual(proc.returncode, 0)
            self.assertEqual(decision(proc.stdout.strip()), "allow")

    def test_other_tree_cannot_spawn_into_minimax_lab(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            env = os.environ.copy()
            env["OPENFLOW_SPAWN_STATE"] = tmp
            payload = {
                "hookEventName": "pre_tool_use",
                "cwd": "/home/mitun/open-flow/.worktrees/tree-a",
                "workspaceRoot": "/home/mitun/open-flow/.worktrees/tree-a",
                "toolName": "spawn_subagent",
                "toolInput": {
                    "prompt": "CAVEMAN. DID: x\nPASS-FAIL: n/a\nNEXT: y",
                    "cwd": "/home/mitun/open-flow/.worktrees/minimax",
                },
            }
            proc = subprocess.run(
                [sys.executable, str(PRETOOL)],
                input=json.dumps(payload),
                text=True,
                capture_output=True,
                check=False,
                env=env,
            )
            self.assertEqual(proc.returncode, 0)
            self.assertEqual(decision(proc.stdout.strip()), "deny")

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
        self.assertIn("plan", ctx.lower())
        self.assertIn("caveman", ctx.lower())
        self.assertRegex(ctx.lower(), r"not apk|no apk")
        self.assertRegex(ctx.lower(), r"small")
        self.assertIn("memory", ctx.lower())
        self.assertIn("5", ctx)
        self.assertRegex(ctx.lower(), r"isolat|jail|this repo|this project")
        self.assertIn("minimax", ctx.lower())


if __name__ == "__main__":
    unittest.main(verbosity=2)
