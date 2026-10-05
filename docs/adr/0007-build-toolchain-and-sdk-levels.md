# 0007: Build toolchain and SDK levels

Date: 2026-10-03

## Status

Accepted

**Amended by [0010](0010-release-artifact-shape.md)** (2026-10-03): the reason recorded
below for `isMinifyEnabled = false` was incomplete. The JNI hazard is real, but it was
never the reason to leave it off — R8's entire reachable surface in this APK is the 1,057,351-byte
`classes.dex`, 0.82% of a 128,850,066-byte artifact, because there is no application code to
strip. ADR-0010 keeps minification off for V1 on the corrected ground.

## Context

Every remaining ticket assumed a Gradle project that did not exist. The `ci` workflow
was green only because it probed for `gradlew` and skipped, so no Android build had
ever run in this repository and the toolchain had never been exercised.

Four choices were coupled and had to be settled together, because each constrains the
others: the JDK, the Android Gradle Plugin, the Kotlin version, and the SDK levels.
Two facts shaped them. The sherpa-onnx AAR — this project's first provider — is
compiled against Kotlin 1.7.20 and ships an **empty** `consumer-rules.pro`, and
AGP/Gradle do not bind an AAR's consumer, so the only version constraint it imposes
is that the Kotlin compiler must be able to read 1.7.x metadata. And CI already
pinned JDK 21 while the development machine has JDK 17 and cannot install 21.

`minSdk` is the choice with teeth: it is a shipped promise about which phones
OpenFlow runs on, and the acceptance gate names Samsung-class and Xiaomi-class
hardware. Choosing it for the convenience of the SDK tooling would quietly shrink
the gate the destination depends on.

## Decision

- **Wrapper:** Gradle **8.13** (`-bin` distribution), committed including
  `gradle-wrapper.jar`.
- **AGP 8.13.0, Kotlin 2.0.21**, both declared once in a
  `gradle/libs.versions.toml` version catalogue.
- **`compileSdk` 36, `targetSdk` 36, `minSdk` 26.**
- **Java 17**, set through `compileOptions` and `kotlin { compilerOptions }`, with
  **no `jvmToolchain` block**. Gradle runs on whatever JDK starts it: 21 in CI, 17
  locally. Both emit Java 17 bytecode and both work.
- **CI moved from JDK 21 to JDK 17** in the `ci`, `android-test` and `release`
  workflows, so the build runs on the same JDK everywhere.
- **No core library desugaring.** At `minSdk` 26 the `java.time` API is native, so
  the build declares nothing it does not need.
- **Three modules per ADR-0005** — `app → core ← providers/sherpa` — with
  `RepositoriesMode.FAIL_ON_PROJECT_REPOS` so the dependency rule is enforced by the
  build rather than by review. `core` enables `testFixtures`, which ADR-0006 needs.
- **sherpa-onnx `v1.13.8` from JitPack**, as a tag, never as `master`.
- **Model binaries are never fetched by a build.** The ~43 MB ASR model and ~0.6 MB
  VAD model are runtime assets.
- **`isMinifyEnabled = false`** for now.

### Why these

`minSdk 26` because the acceptance gate's devices are Android 8.0-era hardware.
Going to 21 buys no reach on those OEMs, and it would cost both a desugaring
dependency and a much larger `AudioRecord` quirk matrix to test by hand. It also
lands above sherpa-onnx's own `minSdk` of 21, so the direction of that constraint
works in our favour rather than against us.

JDK 17 rather than 21 because it is what the project can actually be built on, and a
build that only its CI can run is not a build. The Kotlin Gradle Plugin is older than
AGP 8.13 and may warn that AGP is "not fully supported"; that is a warning, and
bumping Kotlin's one version line is the fix if it ever stops being one.

AGP 8.13.0 rather than 8.7.3 because `compileSdk` 36 is only *tested* by the newer
plugin — the older one would need either a `compileSdk` two levels back or an
`android.suppressUnsupportedCompileSdk` flag, to suppress a warning about the choice
already made.

No `jvmToolchain` block rather than toolchain 17 plus a JDK resolver plugin, because
the second buys a guarantee this project does not currently need (that no JDK-21-only
*Java* API creeps in via CI) at the cost of a plugin and a network fetch inside every
CI build. `lint`'s `NewApi` check still covers the surface that actually matters here,
which is the Android API. Tightening this later is a one-line change to a build that
will by then have been verified many times.

`sherpa-onnx` pinned to a tag because upstream's JitPack build configuration
**hardcodes** the version it mirrors; an unpinned coordinate can therefore resolve to
an AAR whose contents do not match what the coordinate claims.

Minification off because JitPack's AAR ships no consumer keep rules while its JNI
layer resolves classes and methods by name (`Java_com_k2fsa_sherpa_onnx_*`). An R8
build would break at runtime in a way nothing in the build would catch.

The reason above is sound but incomplete, and the amendment matters for a future
reader. Turning minification on with no keep rules does not produce a slightly smaller
APK: it produces one with **no `classes.dex` at all**, and `assembleRelease` still
succeeds. The hazard is real, but the reason to leave minification off is that there
is nothing to gain — `core/` has no `src/` directory, so the whole codebase is one
probe file, and the 0.82% of the APK that R8 can reach is not worth any risk to the
0.82% that it cannot. See ADR-0010 for the measured figures.

Room's annotation processor and the Compose compiler plugin are **deliberately
absent**: neither has any code to compile yet, and adding them would add a
version-matched KSP plugin to the toolchain before there is a single entity to map.

## Consequences

The build is green and the `ci` workflow no longer skips. What that gate now proves
is narrower than it looks, and should not be oversold: it proves the toolchain
resolves, the modules assemble, and the sherpa-onnx AAR's Kotlin classes and members
are readable by this compiler — `providers/sherpa` contains one `internal` probe that
names `OnlineRecognizerConfig` and `OnlineStream` for exactly that reason. It runs
**no tests**, because there is no application code to test.

Three consequences worth carrying:

- **The debug APK is ~129 MB.** The AAR carries native libraries for four ABIs and a
  debug build strips nothing. The CI workflow uploads it on every run. Release size
  and ABI splitting were not decided here and are settled in ADR-0010.
- **`gradlew` must not grow a model-download task.** The 50 MB AAR is the largest
  thing a build fetches, and it is fetched from a declared coordinate; the weights
  are not, because a build has to run on a machine with no model cache.
- **If `minSdk` ever drops below 26, desugaring comes back** as a new decision rather
  than as a build error.