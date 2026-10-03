# The Gradle project skeleton

Type: grilling
Status: resolved
Blocked by: none

## Question

Every remaining ticket assumes a Gradle project that does not exist: no `app/`, `core/`, or `providers/`, no `gradlew`, no `settings.gradle.kts`. The dependency rule, the module layout, the three-device acceptance gate, and CI all depend on it. `ci.yml` is written and green *only* because it probes for `gradlew` and skips — the Android build has never run in this repository.

So this is the first decision-shaped thing on the route after the infrastructure. What it has to settle:

- **JDK, Android Gradle Plugin, and Kotlin versions.** These move together and AGP pins a Kotlin range, so they are one choice, not three. Kotlin and AGP both need to be on a version the sherpa-onnx JitPack artifacts were built against — check that before picking, not after.
- **`compileSdk` / `targetSdk` / `minSdk`.** `minSdk` is the one with teeth: it is a hard promise about which devices OpenFlow supports, and the destination's acceptance gate names Samsung-class and Xiaomi-class hardware. A `minSdk` chosen for the SDK tools rather than for the OEM matrix would quietly shrink the gate.
- **Kotlin/JVM target and desugaring.** The refiner is heavy on text manipulation and the router on collection work; `java.time` on older devices needs desugaring, and deciding that now is cheaper than retrofitting it.
- **What each module actually declares.** ADR-0005 fixes `app → core ← providers/*` and the documented seams, but not build files. `core` needs `testFixtures` — ADR-0006 puts the Contract Test suite there, and that choice changes `core`'s build configuration, not just its source layout.
- **Whether model binaries are ever vendored.** Ticket 01 ruled out bundling model binaries, so `gradlew` must not fetch ~45 MB of weights as part of a build. Worth writing down as an explicit non-default before someone helpfully caches it.

Deliverable: a `settings.gradle.kts`, three `build.gradle.kts`, a committed Gradle wrapper, and `gradlew` verified to run. Once that lands, `ci.yml` stops skipping with no edit, and the first real question becomes whether the build is green — which is the honest first test of ADR-0005.

Do not write application code in this ticket. Skeleton, versions, and build configuration only.

## Answer

Landed. `./gradlew lint test assembleDebug` is green from a clean state, `ci` no longer
skips, and the decisions are in `docs/adr/0007-build-toolchain-and-sdk-levels.md`.

### What was decided

- **Gradle 8.13** (wrapper committed, `-bin`), **AGP 8.13.0**, **Kotlin 2.0.21**, all
  declared once in `gradle/libs.versions.toml`.
- **`compileSdk` 36, `targetSdk` 36, `minSdk` 26.** 26 because the acceptance gate's
  Samsung-class and Xiaomi-class hardware is Android 8.0-era; 21 buys no reach on those
  OEMs and would cost both a `java.time` desugaring dependency and a much larger
  `AudioRecord` quirk matrix to test by hand. It also sits above sherpa-onnx's own
  `minSdk` of 21, so the one real constraint points our way. **No desugaring is
  configured**, because at 26 `java.time` is native.
- **JDK 17 everywhere**, and CI was changed to match. This is the owner's call and it
  overrides the recommendation to make the project run on both 17 and 21: `ci`,
  `android-test` and `release` workflows all moved from temurin 21 to 17, so the build
  now runs on the same JDK on this machine as it does in CI. Java 17 is set through
  `compileOptions` and `kotlin { compilerOptions }` with **no `jvmToolchain` block** —
  Gradle runs on whatever JDK starts it and emits Java 17 bytecode either way.
- **AGP 8.13.0 over 8.7.3** because `compileSdk` 36 is only *tested* by the newer
  plugin; the older one would have forced either a two-levels-back `compileSdk` or an
  `android.suppressUnsupportedCompileSdk` flag, to suppress a warning about a choice
  already made.
- **sherpa-onnx `v1.13.8` from JitPack**, pinned to a tag. Upstream's JitPack build
  configuration **hardcodes** the version it mirrors, so an unpinned coordinate can
  resolve to an AAR whose contents contradict the coordinate.
- **One ADR, no second document.** The commands live in `CONTRIBUTING.md`, which
  already promised `./gradlew lint test assembleDebug` must pass — a promise that was
  false until this ticket and is now true.
- **Version catalogue** over per-module version literals, so `compileSdk` and the
  plugin versions are stated once instead of three times.

### What the build deliberately does not have

`app` has a manifest and **no Kotlin**. No Activity, no resources beyond a platform
theme. `providers/sherpa` contains one `internal` probe function — no more than that,
and it is not a working recognizer. Room's annotation processor and the Compose
compiler plugin are absent, because neither has any code to compile yet and Room would
drag a version-matched KSP plugin into the toolchain before there is an entity to map.
`isMinifyEnabled = false`, for a reason below.

### The four module declarations, per ADR-0005 and ADR-0006

`app → core ← providers/sherpa`, with `RepositoriesMode.FAIL_ON_PROJECT_REPOS` so the
dependency rule is a build failure rather than a review comment. `core` enables
`testFixtures` — that is build configuration, not source layout, and ADR-0006's
Contract Test suite needs it. `core` exposes coroutines as **`api`**, not
`implementation`, because `Flow<SpeechEvent>` is the shape of its public contract;
this is the one place ADR-0005's "`api` only for re-exported contract types" rule
applies today.

### What the sherpa investigation changed

The AAR resolves: `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.8`, **50,129,134
bytes**, the only Maven-coordinate channel for it (it is not on Maven Central). JitPack
is a **mirror, not a build** — its config runs `install:install-file` on the AAR that
sherpa-onnx's own release pipeline produced, byte-identical. So **no NDK is needed**,
which removes the largest build risk the ticket was written under. It ships native
libraries for four ABIs and **no transitive dependencies**: the library declares
androidx as `implementation`, so nothing androidx is re-exported.

The one thing that genuinely bit: its **`consumer-rules.pro` is empty** while the JNI
layer resolves classes by name. That is why minification is off, and it is now ticket
[33 What a shipped OpenFlow APK is](33-release-apk-shape.md).

The probe is what makes the dependency *proven* rather than merely declared:
`providers/sherpa/src/main/kotlin/.../SherpaOnnxContractProbe.kt` names
`OnlineRecognizerConfig` and `OnlineStream` and reads a member from each, so the
compiler has to read the AAR's Kotlin metadata (1.7.x) with a 2.0.21 compiler. A
mismatch now fails here instead of surfacing later inside the provider.

### Verification, and its honest limit

`./gradlew clean` then `lint test assembleDebug` → **BUILD SUCCESSFUL**, zero errors,
six lint warnings, all expected: `MissingApplicationIcon` (ticket 17 settled an icon
brief, nothing is drawn), and two "newer version available" notices. `app-debug.apk`
is **129 MB** — four ABIs, nothing stripped. `python3 .github/scripts/check_docs.py`
is green against 35 markdown files with **no baseline file at all**.

**What the gate does not prove: any behaviour.** It runs zero tests, because there is
no application code to test. `CONTRIBUTING.md` now says so in those words rather than
letting a green check imply otherwise. The gate's real content today is: the toolchain
resolves, the modules assemble, and the provider dependency is consumable.

### Graduation and follow-ups

Three new tickets, and one piece of fog graduated because the shell it was waiting for
now exists:

- [32 Correct the false claims in the sherpa-onnx provider docs](32-correct-sherpa-doc-errors.md)
  — `docs/providers/sherpa-onnx.md` and `model-selection.md` name a class
  (`SherpaOnnxRecognizer`) that does not exist, describe copying the Kotlin API into
  the app, overstate Silero VAD 3×, and cite a three-releases-old version. Found
  while building this, deliberately not fixed here: it is documentation, not build
  configuration.
- [33 What a shipped OpenFlow APK is](33-release-apk-shape.md) — minification and its
  missing keep rules, ABI splitting, and a size ceiling someone actually enforces.
- [34 How the bubble overlay window is actually built](34-bubble-overlay-window.md) —
  graduated from the map's fog, now that the shell exists. Carries the
  `POST_NOTIFICATIONS` gap this manifest made visible: `targetSdk` 36 with no such
  permission declared means the foreground-service notification a bubble app depends on
  is invisible on Android 13+.

Ticket 30 (harness model benchmark) was blocked by this one and is now unblocked.