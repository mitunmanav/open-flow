#!/usr/bin/env bash
# Dump crash buffer + fatal lines. Exit 1 if a crash/ANR is present.
set -euo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

OUT="${1:-}"
if [ -n "$OUT" ]; then
  mkdir -p "$(dirname "$OUT")"
fi

dump() {
  echo "=== crash buffer ==="
  qa_adb_s logcat -d -b crash 2>/dev/null || true
  echo "=== fatal / ANR ==="
  qa_adb_s logcat -d *:E 2>/dev/null \
    | grep -E 'FATAL EXCEPTION|ANR in |Fatal signal' || true
}

if [ -n "$OUT" ]; then
  dump | tee "$OUT"
else
  dump
fi

hits="$(dump | grep -cE 'FATAL EXCEPTION|ANR in|Fatal signal' || true)"
if [ "${hits:-0}" -gt 0 ]; then
  echo "FAIL crash-scan hits=$hits"
  exit 1
fi
echo "PASS crash-scan hits=0"
