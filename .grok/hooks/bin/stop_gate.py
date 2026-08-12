#!/usr/bin/env python3
"""open-flow only — Stop gate.

Block end once if agent claims PASS/fixed/done without proof words.
Respect stopHookActive. Fail open on errors.
"""
from __future__ import annotations

import json
import os
import re
import sys


CLAIM = re.compile(
    r"(?i)\b("
    r"pass|passed|fixed|done|shipped|installed|complete|completed|"
    r"tests?\s+green|all\s+green|working\s+now|resolved"
    r")\b"
)

PROOF = re.compile(
    r"(?i)("
    r"gradlew|testDebugUnitTest|assembleDebug|BUILD SUCCESSFUL|"
    r"tests?\s+(passed|ok)|adb\s+install|unit tests?\s+\d|"
    r"PASS-FAIL:\s*PASS|PASS/FAIL:\s*PASS|"
    r"exit\s+0|0\s+failures?"
    r")"
)

REASON_END = re.compile(r"(?i)^(end_turn|end-turn|completion)?$")


def main() -> None:
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)

    event = str(data.get("hookEventName") or data.get("hook_event_name") or "")
    if event not in ("Stop", "stop", ""):
        sys.exit(0)

    reason = str(data.get("reason") or "")
    # Session-end observe fire — do not gate
    if reason in ("channel_closed", "shutdown", "session_end"):
        sys.exit(0)
    if reason and reason not in ("end_turn", "end-turn", "completion", ""):
        # unknown non-end reasons: allow
        if reason not in ("end_turn",):
            pass

    if data.get("stopHookActive") or data.get("stop_hook_active"):
        sys.exit(0)

    msg = str(
        data.get("lastAssistantMessage")
        or data.get("last_assistant_message")
        or data.get("assistant_message")
        or ""
    )
    if not msg.strip():
        sys.exit(0)

    # Only gate when claiming success
    if not CLAIM.search(msg):
        sys.exit(0)
    if PROOF.search(msg):
        sys.exit(0)

    # Soft: also allow pure meta / plan / status without build claims
    if re.search(r"(?i)\b(NEXT:|ASK:|plan only|no code change|read-only)\b", msg):
        if not re.search(r"(?i)\b(fixed|shipped|installed|tests?\s+green)\b", msg):
            sys.exit(0)

    out = {
        "decision": "block",
        "reason": (
            "open-flow Stop gate: you claimed PASS/fixed/done without proof. "
            "Run tests or assemble, cite output, then caveman DID/PASS-FAIL/NEXT. "
            "Skills: Superpowers + android-cli when Android. "
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
