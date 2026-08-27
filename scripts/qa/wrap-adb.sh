#!/usr/bin/env bash
# Point WSL SDK adb at Windows adb.exe so Gradle + `android` CLI see of_win.
# Linux adb starts its own server and cannot reach Windows 127.0.0.1:5037.
# Re-run after `sdkmanager platform-tools` overwrites the binary.
set -euo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

TARGET="$LINUX_SDK/platform-tools/adb"
WIN="$WIN_ADB"

if [ ! -x "$WIN" ]; then
  echo "FAIL: missing Windows adb: $WIN" >&2
  exit 1
fi
if [ ! -e "$TARGET" ]; then
  echo "FAIL: missing SDK adb: $TARGET" >&2
  exit 1
fi

if head -n1 "$TARGET" 2>/dev/null | grep -q '^#!'; then
  echo "READY wrap already $TARGET -> adb.exe"
  "$ROOT/scripts/qa/adb-bridge.sh"
  exit 0
fi

# Drop a Linux adb daemon if it stole :5037 via localhostForwarding.
if [ -x "$TARGET" ]; then
  "$TARGET" kill-server >/dev/null 2>&1 || true
fi

if [ -e "$TARGET.linux" ]; then
  mv -f "$TARGET" "$TARGET.elf.bak"
else
  mv "$TARGET" "$TARGET.linux"
fi

cat > "$TARGET" <<EOF
#!/usr/bin/env bash
exec "$WIN" "\$@"
EOF
chmod 755 "$TARGET"
echo "READY wrapped $TARGET -> adb.exe"
echo "backup: ${TARGET}.linux"

"$ROOT/scripts/qa/adb-bridge.sh"
