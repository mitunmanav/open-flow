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
    r"android\s+(docs|layout|install|info|screen|run)\b|"
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


def _text_from_content(content: object) -> str:
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        bits: list[str] = []
        for part in content:
            if isinstance(part, str):
                bits.append(part)
            elif isinstance(part, dict) and part.get("type") in ("text", None):
                bits.append(str(part.get("text") or ""))
        return "\n".join(bits)
    return ""


def last_assistant_from_transcript(path: str) -> str:
    last = ""
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            for line in fh:
                line = line.strip()
                if not line:
                    continue
                try:
                    obj = json.loads(line)
                except json.JSONDecodeError:
                    continue
                kind = str(obj.get("type") or obj.get("role") or "")
                msg = obj.get("message") if isinstance(obj.get("message"), dict) else obj
                role = str(msg.get("role") or kind)
                if "assistant" not in (kind + role).lower():
                    continue
                last = _text_from_content(msg.get("content"))
    except OSError:
        return ""
    return last


def assistant_message(data: dict) -> str:
    msg = str(
        data.get("lastAssistantMessage")
        or data.get("last_assistant_message")
        or data.get("assistant_message")
        or ""
    )
    if msg.strip():
        return msg
    path = str(data.get("transcript_path") or data.get("transcriptPath") or "")
    if path:
        return last_assistant_from_transcript(path)
    return ""


def should_block(data: dict) -> bool:
    event = str(data.get("hookEventName") or data.get("hook_event_name") or "")
    if event not in ("Stop", "stop", "SubagentStop", "subagent_stop", "subagentStop", ""):
        return False

    reason = str(data.get("reason") or "")
    if reason in ("channel_closed", "shutdown", "session_end"):
        return False

    if data.get("stopHookActive") or data.get("stop_hook_active"):
        return False

    msg = assistant_message(data)
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
            "Run tests, assemble, or android-cli. Cite real output "
            "(gradlew / BUILD SUCCESSFUL / android docs|layout|install). "
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
