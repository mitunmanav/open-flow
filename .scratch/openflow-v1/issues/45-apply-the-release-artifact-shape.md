# Apply the release artifact shape to the build

Type: task
Status: resolved
Blocked by: none

## Question

Not a question — work whose shape is already decided in ADR-0010. Recorded here so the
decisions have an owner and so nobody rediscovers them as an open question.

Decided in [33 The release APK's shape](33-release-apk-shape.md) and recorded in
`docs/adr/0010-release-artifact-shape.md`. Four build changes, none of which the map
previously owned:

1. **`useLegacyPackaging = true`** in `android { packaging { jniLibs { … } } }` in
   `app/build.gradle.kts`. Measured: the release APK goes from 128,850,066 to
   50,977,462 bytes, the native libraries deflating to 39.1%.
2. **Drop the `x86` ABI.** Ship `arm64-v8a`, `armeabi-v7a`, `x86_64` via
   `android.defaultConfig.ndk.abiFilters`. **This must apply to the debug build too** —
   `android-test.yml` runs the instrumented suite on an `x86_64` emulator, so `x86_64` has
   to survive in debug, and nothing in the release shape may be release-only in a way that
   silently narrows what CI can test.
3. **A 50 MB ceiling on the release APK**, as a Gradle task asserting against
   `assembleRelease` output, wired into `ci.yml` as a hard fail. Deliberately not asserted
   against the debug build, which is ~123 MiB by construction and would be a permanently
   red gate.
4. **Stop building and publishing the `.aab`.** `release.yml` currently runs
   `assembleRelease bundleRelease`, publishes the bundle as a first-class release artifact,
   and lists it in `checksums.txt` and the release body. An `.aab` is a Play-delivery
   format that no user can install and only Play can split, and ADR-0010 settles the
   distribution channel as GitHub Releases.

Two things to carry while doing it:

- **The build file's own comment is now wrong.** `app/build.gradle.kts` explains
  `isMinifyEnabled = false` as "a decision with its own ticket". That ticket has now been
  taken and the decision is recorded in ADR-0010; update the comment so it points at the
  ADR rather than at an open question.
- **The 50 MB number is a bound, not a forecast.** The projected size is ~37.9 MB, computed
  from measured per-ABI compressed sizes plus a 1,057,351-byte dex in an app that has no
  application code. The task must fail *loudly and informatively* when the ceiling is
  crossed — naming the actual size and the ceiling — because its real job is catching a
  re-bundled 45 MB ASR model, which lands near 80 MB.

## Answer

All four changes applied; the release build measured that they work.

1. **`useLegacyPackaging = true`** in `app/build.gradle.kts` under
   `packaging.jniLibs`. First real release APK: **37,394,900 bytes** (was
   128,850,066), native libraries deflating to ~39% — close to ADR-0010's
   projected 37.9 MB, which was arithmetic on a codebase with no application
   code, as the ADR said it would be.
2. **`abiFilters` = `arm64-v8a`, `armeabi-v7a`, `x86_64`** in `defaultConfig`,
   so it applies to debug too. Verified the debug APK still carries all three —
   `x86_64` survives for `android-test.yml`'s emulator, `x86` is gone from both
   variants.
3. **50 MB ceiling** is a `doLast` assertion on `assembleRelease` and
   `packageRelease` in `app/build.gradle.kts`: failure names the actual size and
   the ceiling and points at ADR-0010's hybrid-delivery rule. `ci.yml` now runs
   `assembleRelease -Popenflow.allowUnsignedRelease=true` as a hard fail, so the
   ceiling is exercised on every PR.
4. **`.aab` dropped** from `release.yml`: the assemble step, checksums, upload
   artifact, release files, and the header comment no longer mention it.
   `docs/adr/0012-release-signing.md`'s "on its way out" line updated to
   record the removal. The `isMinifyEnabled = false` comment now points at
   ADR-0010 instead of an open question.

Verification: `:app:lint :app:test :app:assembleDebug` green;
`assembleRelease` green with the ceiling line logging
`app-release-unsigned.apk: 37394900 bytes of a 52428800-byte ceiling`. Caveat:
a repo-wide `lint test assembleDebug` is red in `:providers:sherpa` because
ticket 40's uncommitted benchmark source set does not compile — that untracked
tree belongs to the claimed ticket 40 session and was left alone.