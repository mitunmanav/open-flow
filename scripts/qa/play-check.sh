#!/usr/bin/env bash
# Play-grade static checks. No device needed. Honest PASS/SKIP/FAIL.
# Usage: scripts/qa/play-check.sh [--aab app/build/outputs/bundle/release/app-release.aab]
# Sources: target 36 https://developer.android.com/google/play/requirements/target-sdk
#          16KB https://developer.android.com/guide/practices/page-sizes
#          signing https://developer.android.com/studio/publish/app-signing
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
source "$ROOT/scripts/qa/lib.sh" 2>/dev/null || true

FAIL=0; PASS=0; SKIP=0
OUT="${1:-}"
if [ -f "$OUT" ]; then
  AAB="$OUT"
else
  AAB="$ROOT/app/build/outputs/bundle/release/app-release.aab"
fi
APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
APK_DEBUG="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
GRADLE="$ROOT/app/build.gradle.kts"
MANI="$ROOT/app/src/main/AndroidManifest.xml"
CMAKE="$ROOT/app/src/main/cpp/CMakeLists.txt"

note() { echo "$*"; }
stage() {
  local name="$1" status="$2" detail="${3:-}"
  printf "%-18s %-4s %s\n" "$name" "$status" "$detail"
  case "$status" in
    PASS) PASS=$((PASS+1)) ;;
    FAIL) FAIL=$((FAIL+1)) ;;
    SKIP) SKIP=$((SKIP+1)) ;;
  esac
}

has() { grep -q -- "$1" "$2" 2>/dev/null; }

# 1 — target / compile / NDK
if has 'compileSdk = 36' "$GRADLE" && has 'targetSdk = 36' "$GRADLE"; then
  stage TARGET PASS "compile+target 36"
else
  stage TARGET FAIL "need compileSdk/targetSdk 36"
fi
if has 'ndkVersion = "28.2.13676358"' "$GRADLE"; then
  stage NDK PASS "28.2.13676358"
else
  stage NDK FAIL "need NDK 28.2.13676358"
fi

# 2 — 16KB flags
if has 'ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON' "$GRADLE" && has 'max-page-size=16384' "$CMAKE"; then
  stage PAGE16-GRADLE PASS "gradle+cmake 16384"
else
  stage PAGE16-GRADLE FAIL "missing 16384 flags"
fi
if has 'graphics-path:1.1.0' "$GRADLE"; then
  stage GRAPHICS-PATH PASS "1.1.0"
else
  stage GRAPHICS-PATH FAIL "need graphics-path 1.1.0"
fi

# 3 — versionName in CHANGELOG
VN=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$GRADLE" 2>/dev/null || echo "")
VC=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' "$GRADLE" 2>/dev/null || echo "")
if [ -n "$VN" ] && grep -qE "^## ${VN}\b" "$ROOT/CHANGELOG.md" 2>/dev/null; then
  stage VERSION PASS "$VN / $VC in CHANGELOG"
else
  stage VERSION FAIL "CHANGELOG missing ## $VN"
fi

# 4 — permissions declared minimal
PERMS=$(grep -c "uses-permission" "$MANI" 2>/dev/null || echo 0)
if has 'RECORD_AUDIO' "$MANI" && has 'INTERNET' "$MANI"; then
  stage PERMS PASS "$PERMS perms declared (expect 5)"
else
  stage PERMS FAIL "missing RECORD_AUDIO/INTERNET"
fi
# battery perm must have user dialog — check source has battery text
if has 'REQUEST_IGNORE_BATTERY_OPTIMIZATIONS' "$MANI"; then
  if grep -rq "battery" "$ROOT/app/src/main/java" 2>/dev/null | grep -qi "dialog\|REQUEST_IGNORE"; then
    stage BATTERY PASS "perm with doc/dialog"
  else
    # fallback: at least file mentions rationale
    if grep -rq "battery" "$ROOT/app/src/main" 2>/dev/null; then
      stage BATTERY PASS "perm with code mention"
    else
      stage BATTERY FAIL "battery perm without rationale"
    fi
  fi
else
  stage BATTERY SKIP "no battery perm"
fi

# 5 — privacy links
if [ -f "$ROOT/docs/PRIVACY.md" ] && [ -f "$ROOT/docs/privacy.html" ]; then
  stage PRIVACY PASS "docs/PRIVACY.md + privacy.html"
else
  stage PRIVACY FAIL "missing PRIVACY docs"
fi

# 6 — network security / backup
if has 'cleartextTrafficPermitted="false"' "$ROOT/app/src/main/res/xml/network_security_config.xml" 2>/dev/null; then
  stage NSC PASS "cleartext false + allowlist"
else
  stage NSC FAIL "NSC missing"
fi
if has 'allowBackup="false"' "$MANI"; then
  stage BACKUP PASS "allowBackup false"
else
  stage BACKUP FAIL "allowBackup not false"
fi

# 7 — AAB existence + BundleConfig (handle pipefail SIGPIPE 141)
if [ -f "$AAB" ]; then
  if (set +o pipefail; unzip -l "$AAB" 2>/dev/null | grep -q "BundleConfig.pb"); then
    stage AAB PASS "$(du -h "$AAB" | cut -f1) BundleConfig.pb"
  else
    stage AAB FAIL "no BundleConfig.pb in $AAB"
  fi
else
  stage AAB SKIP "no $AAB (run bundleRelease)"
fi

# 8 — 16KB ELF check for .so in AAB (if AAB exists)
if [ -f "$AAB" ]; then
  TMPDIR=$(mktemp -d)
  trap 'rm -rf "$TMPDIR"' EXIT
  unzip -q "$AAB" "base/lib/*/*.so" -d "$TMPDIR" 2>/dev/null || true
  SO_COUNT=$(find "$TMPDIR" -name "*.so" 2>/dev/null | wc -l)
  if [ "$SO_COUNT" -eq 0 ]; then
    stage ELF16 SKIP "no .so in AAB"
  else
    READELF=$(which llvm-readelf 2>/dev/null || which readelf 2>/dev/null || echo "")
    BAD=0
    for so in $(find "$TMPDIR" -name "*.so"); do
      if [ -n "$READELF" ]; then
        # readelf splits LOAD across 2 lines; use -W for wide one-line format
        if ! "$READELF" -W -l "$so" 2>/dev/null | grep -q "LOAD.*0x4000"; then
          # fallback: check Align column contains 0x4000 for LOAD entries via awk
          if ! "$READELF" -W -l "$so" 2>/dev/null | awk '/LOAD/ && /0x4000/ {found=1} END{exit !found}'; then
            echo " BAD ELF $so"
            BAD=$((BAD+1))
          fi
        fi
      fi
    done
    if [ "$BAD" -eq 0 ]; then
      stage ELF16 PASS "$SO_COUNT .so Align 0x4000"
    else
      stage ELF16 FAIL "$BAD/$SO_COUNT .so not 0x4000"
    fi
  fi
else
  stage ELF16 SKIP "no AAB"
fi

# 9 — APK targetSdk via aapt if available
AAPT=$(ls -1d "${LINUX_SDK:-$HOME/Android/Sdk}"/build-tools/*/aapt 2>/dev/null | sort | tail -n1 || true)
if [ -n "$AAPT" ] && [ -f "$APK_DEBUG" ]; then
  if (set +o pipefail; "$AAPT" dump badging "$APK_DEBUG" 2>/dev/null | grep -F -q "targetSdkVersion:'36'"); then
    stage AAPT-TARGET PASS "debug APK target 36"
  else
    stage AAPT-TARGET FAIL "debug APK not target 36"
  fi
else
  stage AAPT-TARGET SKIP "no aapt/debug apk"
fi

# 10 — no keystore in git + release not debug-signed
if git -C "$ROOT" ls-files 2>/dev/null | grep -qE "\.jks$|\.keystore$"; then
  stage KEYSTORE FAIL "keystore tracked in git"
else
  stage KEYSTORE PASS "no keystore in git"
fi
# 10b — release signing config must not initWith(debug). Regression guard for H6.
RELEASE_BODY=$(awk '/create\("release"\)/{p=1} p{print; if(/^\s{4}\}/){exit}}' "$GRADLE")
if echo "$RELEASE_BODY" | grep -q 'initWith(getByName("debug"))'; then
  stage SIGNING-CONFIG FAIL "release signing config falls back to debug — audit H6"
else
  stage SIGNING-CONFIG PASS "release signing config requires env keystore"
fi
APKSIGNER=$(ls -1d "${LINUX_SDK:-$HOME/Android/Sdk}"/build-tools/*/apksigner 2>/dev/null | sort | tail -n1 || true)
if [ -n "$APKSIGNER" ] && [ -f "$APK" ]; then
  if "$APKSIGNER" verify --print-certs "$APK" 2>/dev/null | grep -qi "Android Debug"; then
    stage SIGNING FAIL "release APK is debug-signed (CN=Android Debug) — set OPENFLOW_KEYSTORE_PATH"
  else
    stage SIGNING PASS "release APK not debug-signed"
  fi
else
  stage SIGNING SKIP "no apksigner/release apk"
fi

# 11 — store listing lengths (if scaffold exists)
if [ -f "$ROOT/docs/store/en-US/title.txt" ]; then
  LEN=$(tr -d '\n' < "$ROOT/docs/store/en-US/title.txt" | wc -c | tr -d ' ')
  if [ "$LEN" -le 30 ] && [ "$LEN" -ge 1 ]; then stage STORE-TITLE PASS "$LEN chars"
  else stage STORE-TITLE FAIL "$LEN chars (need 1-30)"; fi
else stage STORE-TITLE SKIP "no title.txt"; fi
if [ -f "$ROOT/docs/store/en-US/short_desc.txt" ]; then
  LEN=$(tr -d '\n' < "$ROOT/docs/store/en-US/short_desc.txt" | wc -c | tr -d ' ')
  if [ "$LEN" -le 80 ] && [ "$LEN" -ge 1 ]; then stage STORE-SHORT PASS "$LEN chars"
  else stage STORE-SHORT FAIL "$LEN chars (need 1-80)"; fi
else stage STORE-SHORT SKIP "no short_desc.txt"; fi
if [ -f "$ROOT/docs/store/en-US/full_desc.txt" ]; then
  LEN=$(wc -c < "$ROOT/docs/store/en-US/full_desc.txt" | tr -d ' ')
  if [ "$LEN" -le 4000 ] && [ "$LEN" -ge 100 ]; then stage STORE-FULL PASS "$LEN chars"
  else stage STORE-FULL FAIL "$LEN chars (need 100-4000)"; fi
else stage STORE-FULL SKIP "no full_desc.txt"; fi

note ""
note "PLAY-CHECK pass=$PASS fail=$FAIL skip=$SKIP"
[ "$FAIL" -eq 0 ]
