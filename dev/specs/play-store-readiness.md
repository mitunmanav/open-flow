# Spec: Play Store readiness (as if launch)

Status: draft 2026-08-27. Goal: GitHub stays sideload primary, but repo passes **as if** Play submission tomorrow.
Sources: [target API 36 req](https://developer.android.com/google/play/requirements/target-sdk) · [16 KB page size](https://developer.android.com/guide/practices/page-sizes) · [App Bundle + signing](https://developer.android.com/studio/publish/app-signing) · [Data safety](https://developer.android.com/privacy-and-security/declare-data-use) · [Upload bundle](https://developer.android.com/studio/publish/upload-bundle) · [Play publishing checklist 2026](https://www.applaunchflow.com/blog/google-play-store-submission-checklist-2026)

## Objective

Make `main` shippable to Play Console with **no code-behind** at submission time.
- No functional change to dictation/bubble/STT. This is policy + build + repo hygiene.
- Keep debug APK for GitHub, add Play-grade AAB path.
- One command proves it: `scripts/qa/gate.sh` + Play pre-checks.

User stories:
- Maintainer can tag `vX.Y.Z` and get a signed GitHub AAB/APK + release notes without secrets in repo.
- Reviewer can run one gate and see targetSdk 36, 16 KB ELF, bundle, data-safety consistency, and store assets checklist green.
- Play reviewer sees consistent Data safety ↔ privacy policy ↔ manifest permissions ↔ actual SDK sends.

## ASSUMPTIONS I'M MAKING

1. Package `app.openflow` stays. `minSdk 26`, `targetSdk 36`, `compileSdk 36` frozen for this work.
2. Play account type = **personal** (12-testers rule applies). Org path documented but not gated.
3. Signing: `debug` for GitHub, `localRelease` → real upload key lives only in CI secret / local `~/.gradle/gradle.properties`, never committed. Play App Signing enabled for new apps (Google holds app key). Validity > 2033-10-22.
4. No new permissions beyond current 5. `INTERNET` stays declared-but-unused until user picks net ear/brain.
5. No new native ABIs beyond `arm64-v8a` + `x86_64` (emu). 16 KB alignment required for every `.so`.
6. Store assets stay in git under `docs/store/` + fastlane-style `fastlane/metadata/` if needed, not in Play only.

→ Correct me now or I proceed with these.

## Tech Stack

- AGP 8.13, Gradle 8.13, Kotlin 2.x, compileSdk 36, NDK 28.2.13676358.
- Build outputs: `assembleDebug` (APK, sideload) + `bundleRelease` (AAB, Play). `aapt`/`bundletool` for verification.
- CI: GitHub Actions `ubuntu-latest`, Temurin 17, `gradle/actions/setup-gradle@v4`.
- Gate: `scripts/qa/gate.sh` (existing) + new `scripts/qa/play-check.sh`.

## Commands

```
# local fast path (WSL + Windows of_win via wrap)
scripts/qa/wrap-adb.sh
scripts/qa/emu-up.sh up         # of-emu alias
./gradlew :app:testDebugUnitTest :app:lintDebug --console=plain
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
./gradlew :app:bundleRelease --console=plain

# full gate
scripts/qa/gate.sh              # unit+lint+apk+install+instrument+crash
scripts/qa/gate.sh --quick      # skip unit/lint after CI
scripts/qa/gate.sh --release    # also assembleRelease + bundleRelease
scripts/qa/play-check.sh        # Play-grade verifiers (see Success Criteria)

# Play-ish local install
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/bundle/release/app-release.aab  # via bundletool
```

## Project Structure

```
app/build.gradle.kts            compileSdk/targetSdk 36, versionCode/name, signingConfigs, NDK, bundle
app/src/main/AndroidManifest.xml permissions + queries + services + allowBackup
app/proguard-rules.pro          R8 keep for Room/whisper
app/src/main/res/xml/           network_security_config.xml, backup_rules.xml, data_extraction_rules.xml
scripts/qa/                     wrap-adb.sh, adb-bridge.*, emu-up.sh, prep.sh, crash-scan.sh, gate.sh, play-check.sh (new)
docs/specs/play-store-readiness.md  this spec
docs/store/                     icon 512, feature 1024x500, screenshots, listing texts (new)
docs/PRIVACY.md + docs/privacy.html  privacy policy (source of truth)
.github/workflows/              ci.yml, release.yml, pages.yml
fastlane/metadata/android/en-US/ (optional) title/short/full/desc
```

## Code Style

No code style change. Existing rule: `object` policy, Truth tests.

New shell: `set -euo pipefail`, `scripts/qa/lib.sh` shared paths, honest SKIP not fake PASS.

Example verifier snippet:

```bash
# play-check.sh pattern
aapt dump badging "$APK" | grep -q "targetSdkVersion:'36'"
unzip -l "$AAB" | grep -q "BundleConfig.pb"
# 16KB
llvm-readelf -l "$SO" | grep -q "LOAD.*0x4000"
```

## Testing Strategy

- Keep existing: 235 Truth unit tests + `GoldenCorpusTest` + lint. `UiPathTest`/`QaLoopScanTest` pin tags + QA loop.
- Gate stays `am instrument` via Windows adb (of_win 16 KB, API 36). No Robolectric, no extra AVDs unless risk-order demands.
- New `play-check.sh` is **static** pre-checks, runnable without device: AAB exists, versionCode bump, targetSdk, permissions declared→justified, privacy link reachable, Data safety ↔ manifest consistency, 16 KB ELF, signing validity, bundle size.
- Pre-launch report equivalent locally: `lintVitalRelease` + `android test` + manual checklist.

## Boundaries

Always:
- Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` before commit.
- Keep `INTERNET` declaration honest. Every net host must be HTTPS except `localhost`/`10.0.2.2`/RFC1918 literals in NSC.
- Keep `allowBackup=false` + backup_rules exclude all. No backup of prefs/DB.
- Stage only explicit paths, no `git add -A`. Author Mitun only, no Co-Authored-By.
- `scripts/qa/gate.sh` never marks skipped work as PASS (prints SKIP).

Ask first:
- Changing `targetSdk`/`minSdk`/`compileSdk`, NDK version, permissions, signing, or adding SDKs (analytics/ads).
- Adding Play Console API or automated publishing.

Never:
- Commit keystores, `local.properties`, `.env`, `google-services.json`, upload keys, or `aab`/`apk` binaries.
- Add `cleartextTrafficPermitted=true` globally.
- Switch WSL emulator to SwiftShader/lavapipe for daily gate; keep Windows `of_win -gpu host` (`AGENTS.md`).
- Mark untested functional/a11y/visual/perf as PASS.

## Success Criteria (testable)

1. **Target / compile**: `app/build.gradle.kts` `compileSdk 36`, `targetSdk 36`, `minSdk 26`, `ndkVersion 28.2.13676358`.
2. **16 KB**: every `*.so` in AAB/APK `LOAD` align `0x4000` (16384). `graphics-path:1.1.0`, `ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON`, CMake `max-page-size=16384`.
3. **Bundle**: `bundleRelease` produces `app-release.aab` with `BundleConfig.pb`; `assembleRelease` still works; `aapt` `targetSdkVersion:'36'`.
4. **Signing**: `localRelease` uses `release` buildType `minify true`, R8, keys outside repo (CI secret or `~/.gradle`). Cert validity > 2033. Play App Signing doc referenced in `docs/store/SIGNING.md`.
5. **Permissions**: `AndroidManifest.xml` declares only 5 perms; `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` shows user dialog before request (Play policy); each perm has in-app Disclosure traceable to Data safety.
6. **Privacy / Data safety**: `docs/PRIVACY.md` + `docs/privacy.html` + in-app `Legal` screens contain privacy link; `Data safety` draft in `docs/store/DATA_SAFETY.md` lists every perm + SDK (none) + encryption-in-transit true + data deletion path (no account → local wipe). Form consistent with manifest + code sends (system STT may send audio, cloud brain POST only after user pick).
7. **Store listing**: `docs/store/` has 512 icon, 1024x500 feature, ≥2 screenshots 1080p, title ≤30, short ≤80, full ≤4000, no promo claims. `fastlane` or markdown draft committed.
8. **CI**: `.github/workflows/ci.yml` builds `assembleDebug` + `bundleRelease` + `lint` + secret scan + authorship + 16 KB check + play-check; `release.yml` on `v*` attaches `*.aab` + `*.apk` + mapping + notes from `CHANGELOG.md`.
9. **Gate**: `scripts/qa/gate.sh --release` green: WRAP-ADB, EMU, UNIT, LINT, BUILD-DEBUG, BUILD-RELEASE, BUNDLE, APK-INFO, INSTALL, INSTRUMENT, CRASH, PLAY-CHECK. No SKIP masquerading as PASS.
10. **Docs**: `CHANGELOG.md` has `## x.y.z` for `versionName`; `docs/testing.md` gate docs updated; `README.md` + `CONTRIBUTING.md` point to `scripts/qa/gate.sh`.

## Open Questions

- Play account is personal vs org? (affects D-U-N-S + 12/14 closed test timeline).
- Upload key generation: create new or reuse existing? (If new, Play App Signing enroll first).
- Do we publish to GitHub Releases as AAB + APK + mapping, or APK only for now?

## Plan ref

`tasks/plan.md` stays Offline Smart. This work tracks in `tasks/todo-play.md` (next).
