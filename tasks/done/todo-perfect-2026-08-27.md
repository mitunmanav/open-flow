# Todo — Perfect-2026-08-27

Goal: make `app.openflow` production-ready. Audit drove 8 High + many Medium.

Plan: `tasks/plan-perfect.md`. Audit: `OPENFLOW_PRODUCTION_AUDIT.md`.

## P0 — Release blockers

- [ ] H6: Release artifacts must NOT be signed with public debug key
  - Acceptance: `localRelease` removed from `release` buildType; `release` uses real env keystore; `play-check.sh` fails on `CN=Android Debug` in any artifact.
  - Verify: `apksigner verify --print-certs app-release.apk` shows non-debug DN when env keystore present, errors when absent.
  - Files: `app/build.gradle.kts`, `scripts/qa/play-check.sh`
- [ ] H8: Keystore wrap failure must NOT crash caller
  - Acceptance: `SecretStore.getOrCreate()` returns null on missing wrap key; callers handle null and surface honest notice.
  - Verify: Truth tests on `SecretStore` null path.
  - Files: `secrets/SecretStore.kt`, callers in `secrets/AndroidSecretStore.kt`

## P1 — Honesty / UX fixes

- [ ] H7: Undo chip must follow `canUndo`, not always false
  - Acceptance: `PostStopChips.state.undo = canUndo`; tests pin this behavior; layout + string already exist.
  - Verify: `PostStopChipsTest` green with undo true path.
  - Files: `bubble/PostStopChips.kt`, `bubble/PostStopChipsTest.kt`
- [ ] H1: Cloud-fatal toast copy + routing
  - Acceptance: notice copy honest about manual ear; `SttRouter.pick(auto = true)` consulted after fatal cloud failure on bubble path.
  - Verify: `CloudFallbackNoticeTest` covers new copy; integration note in plan.
  - Files: `stt/CloudFallbackNotice.kt`, `FlowAccessibilityService.kt`
- [ ] H5: Battery step "seen" only after grant
  - Acceptance: `setupBatterySeen = true` only when `PowerManager.isIgnoringBatteryOptimizations == true` on `ON_RESUME`, or on explicit Skip.
  - Verify: manual trace + Truth test on `FirstRunPolicy.step` advance.
  - Files: `ui/MainActivity.kt`, `ui/setup/FirstRunPolicy.kt`

## P2 — Polish

- [ ] M1: Pre-API-33 dynamic receivers exported explicitly
  - Acceptance: in-process-only copy via `LocalBroadcastManager` or signature permission; or fall back to in-process handlers (no dynamic receiver below 33).
  - Verify: lint clean on debug.
  - Files: `bubble/ReceiverExportPolicy.kt`, `FlowAccessibilityService.kt`

## Checkpoint

- [ ] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` green
- [ ] `bash scripts/qa/play-check.sh` 17 PASS
- [ ] `apksigner verify --print-certs app-release.apk` non-debug DN when keystore env set
