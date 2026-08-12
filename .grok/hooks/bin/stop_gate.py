#!/usr/bin/env python3
"""open-flow only — Stop / SubagentStop gate.

Block end once if agent claims PASS/fixed/done without real command proof.
"PASS-FAIL: PASS" and "NEXT:" are not proof.
Respect stopHookActive. Fail open on errors.
"""
from __future__ import annotations

import json
import re
import sys

PROOF = re.compile(
    r"(?i)("
    r"gradlew|testDebugUnitTest|assembleDebug|BUILD SUCCESSFUL|"
    r"adb\s+install|"
    r"Ran\s+\d+\s+tests?|"
    r"tests?\s+(passed|ok)|"
    r"\b\d+\s+passed\b|"
    r"0\s+failures?"
    r")"
)

OTHER_CLAIM = re.compile(
    r"(?i)\b("
    r"fixed|done|shipped|complete|completed|"
    r"tests?\s+green|all\s+green|working\s+now|resolved"
    r")\b"
)

NA_LINE = re.compile(
    r"(?i)PASS[-/]FAIL:\s*(FAIL|N/?A|NONE|-|PENDING)\b[^\n]*"
)


def is_success_claim(msg: str) -> bool:
    stripped = NA_LINE.sub("", msg)
    if re.search(r"(?i)PASS[-/]FAIL:\s*PASS\b", stripped):
        return True
    if re.search(r"(?i)\bPASS(?:ED)?\b", stripped):
        return True
    return bool(OTHER_CLAIM.search(stripped))


def should_block(data: dict) -> bool:
    event = str(data.get("hookEventName") or data.get("hook_event_name") or "")
    if event not in ("Stop", "stop", "SubagentStop", "subagent_stop", "subagentStop", ""):
        return False

    reason = str(data.get("reason") or "")
    if reason in ("channel_closed", "shutdown", "session_end"):
        return False

    if data.get("stopHookActive") or data.get("stop_hook_active"):
        return False

    msg = str(
        data.get("lastAssistantMessage")
        or data.get("last_assistant_message")
        or data.get("assistant_message")
        or ""
    )
    if not msg.strip():
        return False
    if not is_success_claim(msg):
        return False
    if PROOF.search(msg):
        return False
    return True


def main() -> None:
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)

    if not should_block(data):
        sys.exit(0)

    out = {
        "decision": "block",
        "reason": (
            "open-flow Stop gate: claimed PASS/fixed/done without proof. "
            "Run tests or assemble. Cite real output "
            "(gradlew / BUILD SUCCESSFUL / Ran N tests). "
            "PASS-FAIL: PASS is not proof. "
            "See .grok/rules/00-dev-gate.md"
        ),
    }
    sys.stdout.write(json.dumps(out))
    sys.stdout.flush()
    sys.exit(0)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        sys.exit(0)
