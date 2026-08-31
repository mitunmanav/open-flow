#!/usr/bin/env bash
# Visual harness — capture device screens for manual diff until Paparazzi lands.
# Usage: scripts/qa/visual-capture.sh
# Output: .scratch/visual/<stamp>/{home,insights,setup,bubble}.png + layout json
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
source "$ROOT/scripts/qa/lib.sh" 2>/dev/null || true
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$ROOT/.scratch/visual/$STAMP"
mkdir -p "$OUT"
echo "VISUAL $STAMP -> $OUT"

# ensure device
qa_adb devices 2>/dev/null | grep -q emulator || { echo "SKIP no emulator"; exit 2; }

# prep
bash "$ROOT/scripts/qa/prep.sh" >/dev/null 2>&1 || true

# helper: capture activity screen + layout
cap() {
  local name="$1"
  local extra="${2:-}"
  if [ -n "$extra" ]; then
    qa_adb -s "$SERIAL" shell am start -n "app.openflow.debug/app.openflow.ui.MainActivity" $extra >/dev/null 2>&1 || true
    sleep 2
  fi
  # use android CLI if wrap present, else screencap
  if command -v android >/dev/null 2>&1 && android screen capture -a -o "$OUT/${name}.png" >/dev/null 2>&1; then
    echo "CAP $name screen"
  else
    qa_adb -s "$SERIAL" exec-out screencap -p >"$OUT/${name}.png" 2>/dev/null && echo "CAP $name screencap"
  fi
  # layout dump
  if command -v android >/dev/null 2>&1; then
    android layout --pretty >"$OUT/${name}.layout.json" 2>/dev/null || true
  fi
  qa_adb -s "$SERIAL" shell uiautomator dump "/sdcard/${name}.xml" >/dev/null 2>&1 || true
  qa_adb -s "$SERIAL" pull "/sdcard/${name}.xml" "$OUT/${name}.xml" >/dev/null 2>&1 || true
}

# Home (default)
qa_adb -s "$SERIAL" shell am start -n "app.openflow.debug/app.openflow.ui.MainActivity" >/dev/null 2>&1
sleep 2
cap "home"

# Insights (tap Stats tab)
qa_adb -s "$SERIAL" shell input tap 972 2266 >/dev/null 2>&1; sleep 2
cap "insights"

# Setup: force wizard via prefs? fallback to History for now
qa_adb -s "$SERIAL" shell am start -n "app.openflow.debug/app.openflow.ui.MainActivity" --ez open_history true >/dev/null 2>&1; sleep 2
cap "history"

# Bubble: dump window info + screen with annotate
qa_adb -s "$SERIAL" shell dumpsys window windows 2>/dev/null | grep -A2 "ACCESSIBILITY_OVERLAY" >"$OUT/bubble.window.txt" 2>&1 || true
if command -v android >/dev/null 2>&1; then
  android screen capture -a -o "$OUT/bubble.png" >/dev/null 2>&1 && echo "CAP bubble"
else
  qa_adb -s "$SERIAL" exec-out screencap -p >"$OUT/bubble.png" && echo "CAP bubble screencap"
fi

echo "DONE $OUT"
ls -lh "$OUT" 2>&1 | tail -n 20
