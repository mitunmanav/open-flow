# The Gradle project skeleton

Type: grilling
Status: open
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