#!/usr/bin/env bash
# Windows adb -a (0.0.0.0:5037) + WSL localhost bridge for `android` CLI.
set -euo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

mkdir -p "$SCRATCH"
PIDFILE="$SCRATCH/adb-bridge.pid"
LOG="$SCRATCH/adb-bridge.log"
HOST="$(ip route show default | awk '{print $3}' | head -n1)"

if [ -z "$HOST" ]; then
  echo "FAIL: no default route (Windows host IP)" >&2
  exit 1
fi

listen_all() {
  "$PS" -NoProfile -Command \
    "Get-NetTCPConnection -LocalPort 5037 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty LocalAddress" \
    | tr -d '\r' | grep -qx '0.0.0.0'
}

# Do NOT call adb.linux kill-server after the bridge is up — that packet
# goes to Windows and drops the real server.
if ! listen_all; then
  qa_adb kill-server >/dev/null 2>&1 || true
  sleep 1
  qa_adb -a start-server >/dev/null
  sleep 1
fi
qa_adb -a start-server >/dev/null
if ! qa_adb devices >/dev/null 2>&1; then
  echo "FAIL: Windows adb -a start-server" >&2
  exit 1
fi

if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
  echo "READY adb-bridge pid=$(cat "$PIDFILE") host=$HOST"
  exit 0
fi

if ! timeout 2 bash -c "echo >/dev/tcp/$HOST/5037"; then
  echo "FAIL: Windows $HOST:5037 closed (need adb -a; check firewall)" >&2
  exit 1
fi

nohup python3 "$ROOT/scripts/qa/adb-bridge.py" "$HOST" >>"$LOG" 2>&1 &
echo $! >"$PIDFILE"
sleep 0.3
if ! kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
  echo "FAIL: adb-bridge died — $LOG" >&2
  exit 1
fi
echo "READY adb-bridge pid=$(cat "$PIDFILE") host=$HOST"
