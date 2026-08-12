#!/usr/bin/env bash
set -euo pipefail
cd /home/mitun/open-flow/.worktrees/features-foss-wispr-en
export JAVA_HOME="$HOME/.local/jdk"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew :app:testDebugUnitTest :app:assembleDebug
