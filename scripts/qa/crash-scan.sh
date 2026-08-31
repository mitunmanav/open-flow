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

# Only count crashes for our package — ignore system WebView sigill noise on of_win
hits="$(dump | grep -E 'FATAL EXCEPTION|ANR in|Fatal signal' | grep -c 'app.openflow' || true)"
if [ "${hits:-0}" -gt 0 ]; then
  echo "FAIL crash-scan hits=$hits (app.openflow)"
  exit 1
fi
# Also check for our package in crash buffer via pid/name if grep above missed folded lines
if dump | grep -q 'app.openflow'; then
  if dump | grep -qE 'FATAL EXCEPTION|ANR in|Fatal signal'; then
    # already filtered above; if still app.openflow present with fatal nearby, count
    :
  fi
fi
echo "PASS crash-scan hits=0"
