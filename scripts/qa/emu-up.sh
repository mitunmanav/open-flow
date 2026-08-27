#!/usr/bin/env bash
# Start or reuse Windows AVD of_win with Intel Arc *host* GPU.
# Quick Boot on. Never kill qemu. Never start WSL of_test.
# https://developer.android.com/studio/run/emulator-snapshots
# https://developer.android.com/studio/run/emulator-acceleration#command-gpu
set -euo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

WAIT_S="${OF_EMU_WAIT:-180}"

qemu_up() {
  "$PS" -NoProfile -Command \
    "if (Get-Process qemu-system-x86_64 -ErrorAction SilentlyContinue) { 'yes' } else { 'no' }" \
    | tr -d '\r'
}

device_up() {
  qa_adb devices 2>/dev/null | grep -q "^${SERIAL}[[:space:]]*device"
}

boot_ok() {
  local v
  v="$(qa_adb_s shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  [ "$v" = "1" ]
}

wait_boot() {
  local i=0
  qa_adb wait-for-device >/dev/null 2>&1 || true
  while [ "$i" -lt "$WAIT_S" ]; do
    if device_up && boot_ok; then
      echo "READY serial=$SERIAL avd=of_win gpu=host"
      return 0
    fi
    sleep 2
    i=$((i + 2))
  done
  echo "FAIL: $SERIAL not booted in ${WAIT_S}s" >&2
  return 1
}

cmd="${1:-up}"
case "$cmd" in
  up)
    if device_up && boot_ok; then
      "$ROOT/scripts/qa/adb-bridge.sh"
      echo "READY already serial=$SERIAL"
      exit 0
    fi
    if [ "$(qemu_up)" = "yes" ]; then
      echo "WAIT qemu up, adb not ready yet"
      wait_boot
      exit $?
    fi
    echo "START of_win -gpu host (Quick Boot, no kill)"
    "$PS" -NoProfile -Command \
      "Start-Process -FilePath '${WIN_BAT}' -ArgumentList '-no-metrics'"
    wait_boot
    "$ROOT/scripts/qa/adb-bridge.sh"
    ;;
  wait)
    wait_boot
    ;;
  stop)
    qa_adb emu kill || true
    echo "STOP sent"
    ;;
  status)
    qa_adb devices -l
    if boot_ok; then echo "boot=1"; else echo "boot=0"; fi
    echo "qemu=$(qemu_up)"
    qa_adb_s shell dumpsys SurfaceFlinger 2>/dev/null | grep -m1 GLES || true
    ;;
  save)
    qa_adb emu avd snapshot save qa-ready
    echo "saved qa-ready"
    ;;
  *)
    echo "usage: emu-up.sh [up|wait|stop|status|save]" >&2
    exit 2
    ;;
esac
