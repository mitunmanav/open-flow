#!/usr/bin/env bash
# Device prep for UI tests: no animations, stay awake.
# https://developer.android.com/studio/test/command-line
set -euo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

qa_adb_s shell settings put global window_animation_scale 0
qa_adb_s shell settings put global transition_animation_scale 0
qa_adb_s shell settings put global animator_duration_scale 0
qa_adb_s shell settings put global stay_on_while_plugged_in 3
qa_adb_s shell svc power stayon true >/dev/null 2>&1 || true
qa_adb_s shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
qa_adb_s shell wm dismiss-keyguard >/dev/null 2>&1 || true

echo "READY prep serial=$SERIAL anim=0 stayon"
