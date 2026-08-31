# Todo: Play Store readiness (as if launch)

Spec: `docs/specs/play-store-readiness.md`. Success criteria 1-10.

## Phase 1 — Foundation (commit what exists)

- [x] P1.1 Commit QA loop + 16KB pin (already staged locally)
  - Acceptance: `QaLoopScanTest` green, `readelf -l libggml.so` Align 0x4000, gate PASS.
  - Verify: `./gradlew :app:testDebugUnitTest --tests '*QaLoopScanTest*'` + `gate.sh --quick`
  - Files: `scripts/qa/*`, `app/build.gradle.kts`, `app/src/main/cpp/CMakeLists.txt`, `AGENTS.md`, etc.
  - Size: M

## Phase 2 — Build Play-grade artifacts

- [x] P2.1 AAB wiring + `play-check.sh`
  - Acceptance: `bundleRelease` produces `app-release.aab`; `play-check.sh` checks targetSdk 36, 16KB, version bump, signing placeholder.
  - Verify: `./gradlew :app:bundleRelease --console=plain` + `bash scripts/qa/play-check.sh` — PASS 17/17
  - Files: `app/build.gradle.kts` (already bundle), `scripts/qa/play-check.sh` (new), `.github/workflows/ci.yml`
  - Size: M
  - Depends: P1.1

- [x] P2.2 Release signing docs + localRelease hygiene
  - Acceptance: `docs/store/SIGNING.md` describes Play App Signing, upload key in CI secret, cert >2033, no keystore in repo.
  - Verify: `grep -q "Play App Signing" docs/store/SIGNING.md` + `git grep -l "keystore" || true` empty for committed keys — PASS
  - Files: `docs/store/SIGNING.md` (new), `app/build.gradle.kts` docs comment, `gradle.properties` gitignore check
  - Size: S

- [x] P2.3 CI builds bundle + 16KB + play-check
  - Acceptance: `ci.yml` adds `bundleRelease`, `play-check.sh`, upload `lint-report` + `aab` artifact on main; authorship/secret scan stays.
  - Verify: `cat .github/workflows/ci.yml | grep -q bundleRelease` — PASS
  - Files: `.github/workflows/ci.yml`
  - Size: S
  - Depends: P2.1

## Checkpoint: Foundation+Build

- [x] `gate.sh --release` green (includes BUILD-RELEASE + BUNDLE + PLAY-CHECK) — PLAY-CHECK 14 PASS, gate --quick PASS (BUNDLE/PLAY skipped without --release)
- [x] AAB 16KB verified via `readelf -W -l` Align 0x4000 (15 .so)

## Phase 3 — Policy surfaces (Data safety / privacy / perms)

- [x] P3.1 Data safety draft + manifest trace
  - Acceptance: `docs/store/DATA_SAFETY.md` lists every permission + SDK (none) + encryption true + deletion path; matches manifest + actual sends (system STT, optional brain POST).
  - Verify: `grep -q "RECORD_AUDIO" docs/store/DATA_SAFETY.md` + play-check PERMS/NSC PASS — DONE
  - Files: `docs/store/DATA_SAFETY.md` (new), `scripts/qa/play-check.sh` (extends)
  - Size: M

- [x] P3.2 Privacy policy in-app + Play link consistency
  - Acceptance: `docs/PRIVACY.md` + `docs/privacy.html` + in-app `Legal` already have privacy link; checklist ensures URL reachable and versioned.
  - Verify: `grep -r "privacy" docs/store/` + play-check PRIVACY PASS — DONE
  - Files: `docs/store/PRIVACY_CHECK.md` (new) or extend DATA_SAFETY
  - Size: S

- [x] P3.3 Battery opt + accessibility disclosure audit
  - Acceptance: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` gated behind dialog in `FlowAccessibilityService` docs; a11y string `@string/a11y_service_description` reviewed for Play sensitive-perm policy.
  - Verify: `grep -q "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" app/src/main/AndroidManifest.xml && grep -q "battery" app/src/main/java -r` — DONE, docs/store/SENSITIVE_PERMS.md
  - Files: `docs/store/SENSITIVE_PERMS.md` (new)
  - Size: S

## Phase 4 — Store listing + automation

- [x] P4.1 Store assets scaffold
  - Acceptance: `docs/store/` has `en-US/title.txt` (≤30), `short_desc.txt` (≤80), `full_desc.txt` (≤4000), `icon-512.png` placeholder, `feature-1024x500.png`, 2 screenshots placeholders + spec README.
  - Verify: `wc -c docs/store/en-US/title.txt` 9 ≤30, 58 ≤80, 791 ≤4000 — DONE, play-check STORE-* PASS
  - Files: `docs/store/en-US/*`, `docs/store/README.md` (new)
  - Size: M

- [x] P4.2 Release workflow attaches AAB+APK+mapping
  - Acceptance: `release.yml` on `v*` builds `bundleRelease` + `assembleRelease`, attaches `*.aab` + `*.apk` + `mapping.txt` + notes from CHANGELOG.
  - Verify: `cat .github/workflows/release.yml | grep -q "bundleRelease"` — PASS
  - Files: `.github/workflows/release.yml`
  - Size: S
  - Depends: P2.1

- [x] P4.3 Changelog + version bump gate tightened
  - Acceptance: `ci.yml` already checks CHANGELOG `## versionName`; add `versionCode` monotonic check in `play-check.sh`.
  - Verify: `bash scripts/qa/play-check.sh | grep -q "VERSION"` — PASS
  - Files: `scripts/qa/play-check.sh`
  - Size: XS

## Checkpoint: Policy+Store

- [x] `docs/store/DATA_SAFETY.md` reviewed against manifest — PASS, play-check PERMS/NSC/BACKUP all PASS
- [x] `fastlane` dry-run or asset lint passes — PLAY-CHECK STORE-* 3 PASS

## Phase 5 — Testing expansion (risk order)

- [x] P5.1 Fix functional-check testTag detection
  - Acceptance: `functional-check.sh` uses visible-string proxies for `home_hub`/`nav_bar` instead of `uiautomator` grep which never matches compose testTag.
  - Verify: `bash scripts/qa/functional-check.sh` after `gate.sh --quick` shows PASS 6/6 (was 3/6) — DONE
  - Files: `scripts/qa/functional-check.sh`
  - Size: S

- [x] P5.2 Device coverage doc + matrix note
  - Acceptance: `docs/testing.md` documents `of_win` 16KB API 36 as primary, notes matrix later (foldable/tablet if needed).
  - Verify: `docs/testing.md` now lists `--release` + `play-check.sh` 17 PASS + functional 6/6 — DONE
  - Files: `docs/testing.md` (small edit)
  - Size: XS

## Final gate

- [x] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease` — all 4 green (bundle 12M)
- [x] `scripts/qa/play-check.sh` 17 PASS; `scripts/qa/functional-check.sh` 6 PASS; `scripts/qa/gate.sh --quick` PASS (9 stages) — DONE
- [x] `scripts/qa/gate.sh --release` full (unit+lint+release+bundle+play-check+install+instrument+crash) — PASS 20260827-020245 — DONE
- [x] Report `.scratch/qa/<stamp>/report.txt` attached in PR — `.scratch/qa/20260827-020245/report.txt` exists — DONE

## Risks

| Risk | Impact | Mitigation |
| Battery perm rejection on Play | High | Dialog before request, document use, consider removing if not essential |
| 16KB .so from transitive dep breaks | High | Keep NDK 28 + graphics-path 1.1.0, verify every .so Align 0x4000 in play-check |
| Data safety mismatch (SDK sends) | High | Single source `docs/store/DATA_SAFETY.md` that play-check validates against manifest + okHttp hosts |
| Keystore leak | High | Never commit; CI secret only; gitignore + secret scan |
