# Signing — Play App Signing

Source: [Sign your app](https://developer.android.com/studio/publish/app-signing) · [Upload bundle](https://developer.android.com/studio/publish/upload-bundle)

## GitHub sideload (today)

- `app/build.gradle.kts`: `signingConfigs.release` reads `OPENFLOW_KEYSTORE_PATH` / `_PASS` / `OPENFLOW_KEY_ALIAS` / `OPENFLOW_KEY_PASS` (or matching `openflow.keystore.*` gradle props).
- Missing keystore → **fail loud**. No debug-signed release fallback.
- Local secrets live outside git: `source ~/.openflow/env` (see `~/.openflow/WHY.md`). Never commit `.p12` / passwords.
- `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
- `./gradlew :app:assembleDebug` → debug APK for USB/dev only.
- **Check**: `apksigner verify --print-certs app-release.apk` must show **CN ≠ Android Debug**.

## Play (as-if launch)

- **Format**: `app/build/outputs/bundle/release/app-release.aab` via `./gradlew :app:bundleRelease`. New apps must be AAB (APK not accepted). Bundle contains `BundleConfig.pb`.
- **Play App Signing**: enabled for new apps. Google holds **app signing key** (the identity). You hold **upload key** (signs AAB before upload). If upload key leaks, you can request reset without losing app identity.
- **Upload key**: generate once with `keytool -genkeypair -keystore upload.p12 -storetype PKCS12 -keyalg RSA -keysize 4096 -validity 9125` (Not-after > 2033-10-22). Store **outside repo** (`~/.openflow/`).
- **CI (later)**: GitHub Actions secret base64 of the p12 + same `OPENFLOW_*` env names in `release.yml` (not fully wired yet — local/tag release can upload prebuilt APK).
- **Check**: `apksigner verify --print-certs` · `scripts/qa/play-check.sh` checks keystore not in git + ELF 16KB.
- **Enroll**: first AAB upload in Play Console → Play App Signing enroll → upload key certificate registered.

## What not to do

- Never commit `*.jks`, `*.keystore`, `*.p12`, `upload*`, or `gradle.properties` with passwords. CI secret scan blocks them.
- Never use debug keystore for Play.
- Never reuse `versionCode`; it must increase for every Play upload (play-check validates `VERSION` via `CHANGELOG.md`).

## Quick local check

```
./gradlew :app:bundleRelease
unzip -l app/build/outputs/bundle/release/app-release.aab | grep BundleConfig.pb
readelf -W -l app/build/intermediates/cmake/release/obj/arm64-v8a/libwhisper.so | grep LOAD
bash scripts/qa/play-check.sh
```
