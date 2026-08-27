# Shared paths for this laptop (WSL2 + Windows host GPU emu).
# Sourced by scripts/qa/*.sh — not for login shells.
# Docs: docs/testing.md

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LINUX_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
WIN_USER="${WIN_USER:-$(cmd.exe /c "echo %USERNAME%" 2>/dev/null | tr -d '\r\n')}"
WIN_USER="${WIN_USER:-${USER:-default}}"
WIN_ADB="${WIN_ADB:-/mnt/c/Users/${WIN_USER}/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
WIN_BAT="${WIN_BAT:-C:\\Users\\${WIN_USER}\\.android\\start-of-win.bat}"
WIN_EMU="${WIN_EMU:-/mnt/c/Users/${WIN_USER}/AppData/Local/Android/Sdk/emulator/emulator.exe}"
PS='/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe'
SERIAL="${ANDROID_SERIAL:-emulator-5554}"
PKG_DEBUG="app.openflow.debug"
TEST_PKG="app.openflow.debug.test"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
APK_DEBUG="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
APK_TEST="$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
SCRATCH="$ROOT/.scratch/qa"

qa_adb() {
  "$WIN_ADB" "$@"
}

qa_adb_s() {
  "$WIN_ADB" -s "$SERIAL" "$@"
}
