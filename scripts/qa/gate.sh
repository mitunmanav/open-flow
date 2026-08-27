#!/usr/bin/env bash
# Risk-based device gate. Honest skips. Never mark unrun work as PASS.
# Usage: scripts/qa/gate.sh [--quick] [--release]
# https://developer.android.com/studio/test/command-line
set -uo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

QUICK=0
DO_RELEASE=0
for arg in "$@"; do
  case "$arg" in
    --quick) QUICK=1 ;;
    --release) DO_RELEASE=1 ;;
    -h|--help)
      echo "usage: scripts/qa/gate.sh [--quick] [--release]"
      exit 0
      ;;
  esac
done

STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$SCRATCH/$STAMP"
mkdir -p "$OUT"
REPORT="$OUT/report.txt"
: > "$REPORT"

FAILS=0
note() { printf '%s\n' "$*" | tee -a "$REPORT"; }
stage() {
  local name="$1" status="$2" detail="${3:-}"
  note "$status  $name${detail:+  $detail}"
  if [ "$status" = "FAIL" ]; then
    FAILS=$((FAILS + 1))
  fi
}

run_logged() {
  local log="$1"
  shift
  "$@" >"$log" 2>&1
  return $?
}

cd "$ROOT"
note "GATE  $STAMP"
note "ROOT  $ROOT"
note "SERIAL  $SERIAL"
note "QUICK  $QUICK  RELEASE  $DO_RELEASE"
note ""

# --- wrap SDK adb ---
if "$ROOT/scripts/qa/wrap-adb.sh" >"$OUT/wrap-adb.log" 2>&1; then
  stage WRAP-ADB PASS
else
  stage WRAP-ADB FAIL "see $OUT/wrap-adb.log"
fi

if "$ROOT/scripts/qa/adb-bridge.sh" >"$OUT/adb-bridge.log" 2>&1; then
  stage ADB-BRIDGE PASS "$(tail -n1 "$OUT/adb-bridge.log")"
else
  stage ADB-BRIDGE FAIL "see $OUT/adb-bridge.log"
fi

# --- emu ---
if "$ROOT/scripts/qa/emu-up.sh" up >"$OUT/emu-up.log" 2>&1; then
  stage EMU PASS "$(tail -n1 "$OUT/emu-up.log")"
  "$ROOT/scripts/qa/prep.sh" >"$OUT/prep.log" 2>&1 || true
else
  stage EMU FAIL "see $OUT/emu-up.log"
fi

# --- unit ---
if [ "$QUICK" = 1 ]; then
  stage UNIT SKIP "--quick"
else
  if run_logged "$OUT/unit.log" ./gradlew :app:testDebugUnitTest --console=plain; then
    stage UNIT PASS
  else
    stage UNIT FAIL "see $OUT/unit.log"
  fi
fi

# --- lint ---
if [ "$QUICK" = 1 ]; then
  stage LINT SKIP "--quick"
else
  if run_logged "$OUT/lint.log" ./gradlew :app:lintDebug --console=plain; then
    stage LINT PASS
  else
    stage LINT FAIL "see $OUT/lint.log"
  fi
fi

# --- build ---
if run_logged "$OUT/assemble-debug.log" ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain; then
  stage BUILD-DEBUG PASS
else
  stage BUILD-DEBUG FAIL "see $OUT/assemble-debug.log"
fi

if [ "$DO_RELEASE" = 1 ]; then
  if run_logged "$OUT/assemble-release.log" ./gradlew :app:assembleRelease --console=plain; then
    stage BUILD-RELEASE PASS
  else
    stage BUILD-RELEASE FAIL "see $OUT/assemble-release.log"
  fi
  if run_logged "$OUT/bundle-release.log" ./gradlew :app:bundleRelease --console=plain; then
    stage BUNDLE PASS
  else
    stage BUNDLE FAIL "see $OUT/bundle-release.log"
  fi
  if "$ROOT/scripts/qa/play-check.sh" >"$OUT/play-check.log" 2>&1; then
    stage PLAY-CHECK PASS "$(grep -E "PLAY-CHECK" "$OUT/play-check.log" | tail -n1)"
  else
    stage PLAY-CHECK FAIL "see $OUT/play-check.log"
  fi
else
  stage BUILD-RELEASE SKIP "pass --release"
  stage BUNDLE SKIP "pass --release"
  stage PLAY-CHECK SKIP "pass --release"
fi

# --- apk info ---
AAPT="$(ls -1d "$LINUX_SDK"/build-tools/*/aapt 2>/dev/null | sort | tail -n1 || true)"
SIGNER="$(ls -1d "$LINUX_SDK"/build-tools/*/apksigner 2>/dev/null | sort | tail -n1 || true)"
if [ -f "$APK_DEBUG" ] && [ -n "$AAPT" ]; then
  "$AAPT" dump badging "$APK_DEBUG" >"$OUT/apk-badging.txt" 2>&1 || true
  if [ -n "$SIGNER" ]; then
    "$SIGNER" verify --print-certs "$APK_DEBUG" >"$OUT/apk-certs.txt" 2>&1 || true
  fi
  PKG_LINE="$(grep -m1 "package:" "$OUT/apk-badging.txt" || true)"
  stage APK-INFO PASS "$PKG_LINE"
else
  stage APK-INFO FAIL "missing apk or aapt"
fi

# --- install ---
if [ -f "$APK_DEBUG" ] && [ -f "$APK_TEST" ]; then
  qa_adb_s logcat -c >/dev/null 2>&1 || true
  if qa_adb_s install -r -t "$APK_DEBUG" >"$OUT/install-app.log" 2>&1 \
    && qa_adb_s install -r -t "$APK_TEST" >"$OUT/install-test.log" 2>&1; then
    stage INSTALL PASS
  else
    stage INSTALL FAIL "see $OUT/install-*.log"
  fi
else
  stage INSTALL FAIL "apks missing"
fi

# --- instrumented (existing smoke only) ---
if qa_adb_s shell am instrument -w -r \
  "$TEST_PKG/$RUNNER" >"$OUT/instrument.log" 2>&1; then
  # Activity.RESULT_OK is -1. "INSTRUMENTATION_CODE: -1" is success.
  if grep -q 'FAILURES!!!' "$OUT/instrument.log" \
    || grep -q 'Process crashed' "$OUT/instrument.log"; then
    stage INSTRUMENT FAIL "see $OUT/instrument.log"
  elif grep -qE 'OK \([0-9]+ tests?\)' "$OUT/instrument.log"; then
    stage INSTRUMENT PASS "$(grep -E 'OK \(' "$OUT/instrument.log" | tail -n1)"
  else
    stage INSTRUMENT FAIL "no OK line — see $OUT/instrument.log"
  fi
else
  stage INSTRUMENT FAIL "see $OUT/instrument.log"
fi

# --- crash ---
if "$ROOT/scripts/qa/crash-scan.sh" "$OUT/crash.log" >"$OUT/crash-scan.out" 2>&1; then
  stage CRASH PASS
else
  stage CRASH FAIL "see $OUT/crash.log"
fi

# Honest skips — not tested this run
note ""
note "SKIP  FUNCTIONAL-CORE  not in this gate (bubble/a11y/STT = later)"
note "SKIP  VISUAL           no baselines yet"
note "SKIP  A11Y-AUDIT       later"
note "SKIP  PERF             later"
note "SKIP  MATRIX           one AVD (of_win) only"
note ""

if [ "$FAILS" -eq 0 ]; then
  note "DECISION  PASS  required stages green"
  echo "REPORT $REPORT"
  exit 0
fi
note "DECISION  FAIL  $FAILS required stage(s)"
echo "REPORT $REPORT"
exit 1
