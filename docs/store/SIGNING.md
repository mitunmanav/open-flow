# Signing — Play App Signing

Source: [Sign your app](https://developer.android.com/studio/publish/app-signing) · [Upload bundle](https://developer.android.com/studio/publish/upload-bundle)

## GitHub sideload (today)

- `app/build.gradle.kts` `signingConfigs.debug` + `localRelease` (v1+v2+v3). `debug` keystore is Gradle default, never committed.
- `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (debug-signed, sideload).
- Validity of debug cert is short; not for Play.

## Play (as-if launch)

- **Format**: `app/build/outputs/bundle/release/app-release.aab` via `./gradlew :app:bundleRelease`. New apps must be AAB (APK not accepted). Bundle contains `BundleConfig.pb`.
- **Play App Signing**: enabled for new apps. Google holds **app signing key** (the identity). You hold **upload key** (signs AAB before upload). If upload key leaks, you can request reset without losing app identity.
- **Upload key**: generate once with `keytool -genkeypair -keystore upload.jks -keyalg RSA -keysize 4096 -validity 9125` (> 2033-10-22 required). Store **outside repo**:
  - Local: `~/.gradle/gradle.properties` (`MYAPP_UPLOAD_STORE_FILE`, `MYAPP_UPLOAD_KEY_ALIAS`, passwords) — gitignored.
  - CI: GitHub Actions secret `UPLOAD_KEYSTORE_BASE64` + passwords (never in `.github/workflows/`).
- **Wiring** (when ready): `signingConfigs.create("release")` reads `System.getenv("UPLOAD_KEYSTORE_BASE64")` or `gradle.properties`. `buildTypes.release.signingConfig = signingConfigs.getByName("release")` only when env present; else `assembleRelease` uses `localRelease` (still v1+v2+v3, minify true).
- **Check**: `apksigner verify --print-certs app-release.apk` must show cert `Not after > 2033-10-22`. `scripts/qa/play-check.sh` checks `KEYSTORE` not in git and `ELF16` 16KB.
- **Enroll**: first AAB upload in Play Console → Play App Signing enroll → upload key certificate registered. Afterwards every upload must be signed with same upload key.

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
