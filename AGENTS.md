# Open Flow — Agent Rules

Wispr-Flow-style Android dictation (floating bubble, Kotlin/Compose, accessibility).

## Comms (strict)

- Caveman style. Short lines. Easy words.
- Bullet points only. Brief.
- Report: DID / PASS-FAIL / NEXT / SUGGEST / ASK.
- Wait for GO before file/git changes unless Mitun said fix/build/do it.
- Author: Mitun only. No Co-Authored-By. No agent footers.

## Project structure

- Top-level <20 entries: `app/`, `core/`, `build-logic/`, `dev/`, `docs/`, `scripts/qa/`, `fastlane/`, `gradle/`, `.github/`, `third_party/whisper.cpp`.
- `app/src/main/java/app/openflow/` 17 pkgs. Entrypoints: `OpenFlowApp` → `bubble/FlowAccessibilityService` → `stt/`+`text/`+`audio/`; UI `ui/home|insights|setup|history`.
- `app/build.gradle.kts`: `compileSdk 36 targetSdk 36 minSdk 26 versionName 0.1.9/10 room 2.8.4 ndk 28.2.13676358 graphics-path:1.1.0`. `dist/` `.scratch/` gitignored.
- Specs `dev/specs/*.md`, audits `dev/audit/*.md`, tasks `dev/tasks/active/`, store `docs/store/`, testing `docs/testing.md`. Freshness pinned by `DocsStaleScanTest`+`QaLoopScanTest` (versionName, targetSdk 36, NDK+16384, store title ≤30/short ≤80, privacy links).

## Android workflow

- Use `android-cli` skill for device/SDK/layout/docs. Never `android emulator start` in WSL.
- Device QA: `scripts/qa/gate.sh` (+ `wrap-adb.sh`+`adb-bridge.sh`). Web-search before non-trivial Android/audio/permission/storage decision; cite sources.
- Verify on device/emulator when runtime behavior changes. No blind state-machine refactors.
- Extract small policy/composables. Don't add one-function god files.

## Architecture gates

See `dev/specs/architecture-gates.md`. Do not violate.

- **M3-A** audio tee (`dev/specs/audio-tee-architecture.md` rev2)
- **M7 PARKED** storage privacy (`dev/specs/storage-privacy-tradeoff.md` rev2)
- Bubble / Home / Insights / Setup invariants

## Emulator path

See `dev/specs/architecture-gates.md#emulator-path-wsl2--windows`. WSL has no `/dev/dri` → use Windows AVD `of_win` via `of-emu`. Never `android emulator start of_test` in WSL. Bridge adb: `scripts/qa/wrap-adb.sh`.

## Verify before done

Order: unit → lint → debug APK → (release: AAB + play-check + gate --release + visual).

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
# single test/package
./gradlew :app:testDebugUnitTest --tests "app.openflow.audio.AppAudioCaptureTest"
./gradlew :app:testDebugUnitTest --tests "app.openflow.bubble.*"
# gate
bash scripts/qa/gate.sh --quick
bash scripts/qa/gate.sh --release
bash scripts/qa/play-check.sh     # 17 Play checks
bash scripts/qa/functional-check.sh # 6/6
bash scripts/qa/visual-capture.sh # 4 PNGs + layout JSON → .scratch/
```

Device: `adb devices` → `emulator-5554`, `getprop sys.boot_completed`=1, `dumpsys window` bubble 252×126, no orphan `AudioRecord` after kill-mid-listen. `logcat -s OpenFlow.Probe` for 16k actual/min/frames.

## Code style

- Kotlin/Compose, `Dimen` (`PAGE_PAD` 20 `GAP` 12 `TOUCH_TARGET` 52 `CARD_ROUNDING` 0). Hard-edge, not soft 20dp.
- `Modifier.size(12.dp).background(color, CircleShape)` not `then(Modifier.padding)`. Comments concise.

## Git

- Explicit paths only, never `git add -A`. Author Mitun only, no Co-Authored-By (CI blocks it + secret scan).
- Commits: `feat:`/`fix:`/`ui:`/`setup:`/`qa:`/`docs:` with file paths in body. No Dependabot/bot commits. No push until GO.
- Release `OPENFLOW_KEYSTORE_PATH` etc required for `assembleRelease`/`bundleRelease`; otherwise build fails loud (no debug-signed fallback).

## Docs & Codegraph

- After edits codegraph reindexes ~1s. Use `codegraph_explore` before reading; if banner says `auto-sync DISABLED` read files directly; if `⚠️ Some files...` re-Read those. No manual `codegraph init`.
- Visual harness `scripts/qa/visual-capture.sh` until Paparazzi.

## Skills

- `~/.config/opencode/skills/` per domain: `spec-driven-development`→`planning-and-task-breakdown`→`incremental-implementation`+`test-driven-development`→`code-review-and-quality`→`shipping-and-launch`. Meta-skill `using-agent-skills` maps task→skill.
- Always load `android-cli` for device/SDK/docs; load `compose`/`testing`/`security`/`performance` when domain touches.

## Automation & Releases

- CI `ci.yml`: unit+lint+debug APK+bundleRelease+play-check 17+changelog+secret/authorship+AAB artifact per push/PR (no emulator on ubuntu).
- Release `release.yml`: triggers on push to `main` and `v*` tags. Auto-detects `versionName` bump in `app/build.gradle.kts`, validates changelog, tags `v<version>`, and publishes GitHub release with APK & AAB.
- Pages `pages.yml`: deploys public site from `docs/` on changes.
- Gate `scripts/qa/gate.sh`: stages WRAP-ADB/EMU/UNIT/LINT/BUILD-DEBUG/RELEASE/BUNDLE/PLAY-CHECK/APK-INFO/INSTALL/INSTRUMENT/CRASH.
- Auto-managed `.gitignore`: covers all build outputs, models (`*.bin`), IDEs, secrets, and agent scratches (`.superpowers/`, `.opencode/`, `graphify-out/`).
