# Foundation Modularization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add safe build foundation (version catalog + convention plugins + one pure `:core:common` module) with zero behavior change.

**Architecture:** Single new top-level `core/` dir only. Pure Kotlin module with no Android deps. Old callers keep working via a deprecated typealias shim, so no multi-file edits are needed by consumers.

**Tech Stack:** Gradle 8.x, AGP 8.13.0, Kotlin 2.0.21, Java 17, JUnit4 + Truth (already in repo, no new deps).

**Spec:** `docs/superpowers/specs/2026-09-04-refactor-architecture-design.md`

## Global Constraints

- compileSdk 36, targetSdk 36, minSdk 26, versionName 0.1.9, versionCode 10 — exact, do not change.
- Room 2.8.4, NDK 28.2.13676358, graphics-path 1.1.0, compose-bom 2024.10.01 — exact, do not change.
- No feature removal, no feature addition, no UI redesign, no runtime behavior change.
- Top security, no compromise: no new network/codec deps, no manifest changes in this plan.
- Top-level dir budget: this plan adds only `core/`, `build-logic/`, `gradle/libs.versions.toml`. Nothing else.
- Pure modules (`core:common`) must have zero Android imports. Verify by grep.
- Author: Mitun only. No Co-Authored-By. No agent footers.
- Explicit `git add` paths only. Never `git add -A`.

---

## File Structure

New files this plan creates (nothing else is touched except 2 wiring lines):

- `gradle/libs.versions.toml` — version catalog, values copied verbatim from `app/build.gradle.kts`.
- `build-logic/settings.gradle.kts` — settings for the convention-plugins build.
- `build-logic/build.gradle.kts` — kotlin-dsl for convention plugins.
- `build-logic/src/main/kotlin/openflow.kotlin-pure.gradle.kts` — pure Kotlin module defaults (Java 17, JUnit4 + Truth test deps).
- `build-logic/src/main/kotlin/openflow.android-library.gradle.kts` — Android library defaults (compileSdk 36, minSdk 26, Java 17). NOT applied to any module in this plan; ships ready for later plans.
- `core/common/build.gradle.kts` — applies `openflow.kotlin-pure`.
- `core/common/src/main/kotlin/app/openflow/core/common/TrimPolicy.kt` — moved pure logic, new home.
- `core/common/src/test/kotlin/app/openflow/core/common/TrimPolicyTest.kt` — unit tests (JUnit4 + Truth).
- `app/src/main/java/app/openflow/runtime/TrimPolicy.kt` — MODIFY into a deprecated typealias shim so existing callers (`OpenFlowApp`, `FlowAccessibilityService`) compile unchanged.

---

### Task 1: Version catalog (no behavior change)

**Files:**
- Create: `gradle/libs.versions.toml`
- Modify: none
- Test: none (verify with `gradlew help`)

**Interfaces:**
- Consumes: versions currently hardcoded in `app/build.gradle.kts:150-188`
- Produces: `libs.*` aliases for later plans (this plan does NOT rewrite `app/build.gradle.kts` to use them)

- [ ] **Step 1: Create the catalog file**

```toml
[versions]
agp = "8.13.0"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
compileSdk = "36"
minSdk = "26"
targetSdk = "36"
room = "2.8.4"
coroutines = "1.9.0"
okhttp = "4.12.0"
graphicsPath = "1.1.0"
composeBom = "2024.10.01"
coreKtx = "1.17.0"
activityCompose = "1.11.0"
lifecycle = "2.9.4"
junit4 = "4.13.2"
truth = "1.4.4"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
graphics-path = { group = "androidx.graphics", name = "graphics-path", version.ref = "graphicsPath" }
junit4 = { group = "junit", name = "junit", version.ref = "junit4" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
```

- [ ] **Step 2: Verify catalog parses and app still configures**

Run: `./gradlew help`
Expected: BUILD SUCCESSFUL, no deprecation error about the catalog.

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add version catalog with verbatim current versions

No behavior change. Catalog only, app wiring follows in later plan."
```

---

### Task 2: build-logic convention plugins (not applied yet, except next task)

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/openflow.kotlin-pure.gradle.kts`
- Create: `build-logic/src/main/kotlin/openflow.android-library.gradle.kts`
- Modify: `settings.gradle.kts` (add `includeBuild("build-logic")` in `pluginManagement`)
- Test: none (verify with `gradlew help`)

**Interfaces:**
- Consumes: `gradle/libs.versions.toml` from Task 1
- Produces: plugin ids `openflow.kotlin-pure` and `openflow.android-library` for Tasks 3+ and later plans

- [ ] **Step 1: Create build-logic settings**

```kotlin
// build-logic/settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

- [ ] **Step 2: Create build-logic build file**

```kotlin
// build-logic/build.gradle.kts
plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.13.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
}
```

- [ ] **Step 3: Create pure-Kotlin convention plugin**

```kotlin
// build-logic/src/main/kotlin/openflow.kotlin-pure.gradle.kts
plugins {
    kotlin("jvm")
}

import org.gradle.api.tasks.testing.Test

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    useJUnit()
}

dependencies {
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}
```

- [ ] **Step 4: Create Android-library convention plugin (ships ready, unused in this plan)**

```kotlin
// build-logic/src/main/kotlin/openflow.android-library.gradle.kts
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
    }
}
```

- [ ] **Step 5: Wire includeBuild in root settings**

Modify: `settings.gradle.kts:1-6` — add one line inside `pluginManagement`:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

- [ ] **Step 6: Verify configuration still passes**

Run: `./gradlew help`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add build-logic/settings.gradle.kts build-logic/build.gradle.kts build-logic/src/main/kotlin/openflow.kotlin-pure.gradle.kts build-logic/src/main/kotlin/openflow.android-library.gradle.kts settings.gradle.kts
git commit -m "build: add build-logic convention plugins (pure + android-lib)

Plugins ship ready. Only pure plugin is used by :core:common next."
```

---

### Task 3: `:core:common` module with moved TrimPolicy + tests (TDD)

**Files:**
- Create: `core/common/build.gradle.kts`
- Create: `core/common/src/main/kotlin/app/openflow/core/common/TrimPolicy.kt`
- Create: `core/common/src/test/kotlin/app/openflow/core/common/TrimPolicyTest.kt`
- Modify: `settings.gradle.kts` (add `include(":core:common")`)
- Test: `core/common/src/test/kotlin/app/openflow/core/common/TrimPolicyTest.kt`

**Interfaces:**
- Consumes: plugin `openflow.kotlin-pure` from Task 2
- Produces: `app.openflow.core.common.TrimPolicy` object with functions `action(Int): Action`, `shouldDropIdleStt(Int): Boolean`, `shouldReleaseUiCaches(Int): Boolean`, `dropIdleEngine(level: Int, listening: Boolean, stopInProgress: Boolean = false): Boolean`, constants `TRIM_MEMORY_UI_HIDDEN = 20`, `TRIM_MEMORY_BACKGROUND = 40`, enum `Action { KEEP, RELEASE_UI, DROP_IDLE_STT }` — identical semantics to current `app.openflow.runtime.TrimPolicy`

- [ ] **Step 1: Write the failing test (module does not exist yet, test is the spec)**

```kotlin
// core/common/src/test/kotlin/app/openflow/core/common/TrimPolicyTest.kt
package app.openflow.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrimPolicyTest {
    @Test fun `below 20 keeps`() {
        assertThat(TrimPolicy.action(0)).isEqualTo(TrimPolicy.Action.KEEP)
        assertThat(TrimPolicy.shouldDropIdleStt(0)).isFalse()
        assertThat(TrimPolicy.shouldReleaseUiCaches(0)).isFalse()
    }

    @Test fun `20 releases ui but keeps idle stt`() {
        assertThat(TrimPolicy.action(20)).isEqualTo(TrimPolicy.Action.RELEASE_UI)
        assertThat(TrimPolicy.shouldDropIdleStt(20)).isFalse()
        assertThat(TrimPolicy.shouldReleaseUiCaches(20)).isTrue()
    }

    @Test fun `40 drops idle stt`() {
        assertThat(TrimPolicy.action(40)).isEqualTo(TrimPolicy.Action.DROP_IDLE_STT)
        assertThat(TrimPolicy.shouldDropIdleStt(40)).isTrue()
    }

    @Test fun `never drops while listening or flushing`() {
        assertThat(TrimPolicy.dropIdleEngine(40, listening = true)).isFalse()
        assertThat(TrimPolicy.dropIdleEngine(40, listening = false, stopInProgress = true)).isFalse()
        assertThat(TrimPolicy.dropIdleEngine(40, listening = false)).isTrue()
    }
}
```

- [ ] **Step 2: Create the module build file**

```kotlin
// core/common/build.gradle.kts
plugins {
    id("openflow.kotlin-pure")
}
```

- [ ] **Step 3: Include the module in root settings**

Modify: `settings.gradle.kts:16-17` — append after `include(":app")`:

```kotlin
include(":core:common")
```

- [ ] **Step 4: Run test to verify it fails (no implementation yet)**

Run: `./gradlew :core:common:test`
Expected: FAIL — `TrimPolicy` unresolved.

- [ ] **Step 5: Write minimal implementation (byte-identical logic, new package, no Android imports)**

```kotlin
// core/common/src/main/kotlin/app/openflow/core/common/TrimPolicy.kt
package app.openflow.core.common

/**
 * When to drop idle STT / UI caches on memory pressure.
 * Pure Kotlin. No Android imports.
 * Levels mirror ComponentCallbacks2: UI_HIDDEN = 20, BACKGROUND = 40.
 */
object TrimPolicy {
    const val TRIM_MEMORY_UI_HIDDEN = 20
    const val TRIM_MEMORY_BACKGROUND = 40

    enum class Action { KEEP, RELEASE_UI, DROP_IDLE_STT }

    fun action(level: Int): Action = when {
        level >= TRIM_MEMORY_BACKGROUND -> Action.DROP_IDLE_STT
        level >= TRIM_MEMORY_UI_HIDDEN -> Action.RELEASE_UI
        else -> Action.KEEP
    }

    fun shouldDropIdleStt(level: Int): Boolean = action(level) == Action.DROP_IDLE_STT

    fun shouldReleaseUiCaches(level: Int): Boolean = action(level) != Action.KEEP

    /** Drop idle SpeechRecognizer. Never while listen/flush. */
    fun dropIdleEngine(level: Int, listening: Boolean, stopInProgress: Boolean = false): Boolean =
        shouldDropIdleStt(level) && !listening && !stopInProgress
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :core:common:test`
Expected: BUILD SUCCESSFUL, 4 tests pass.

- [ ] **Step 7: Verify zero Android imports in the new module**

Run: `grep -rn "android\." core/common/src/main/ || echo "CLEAN"`
Expected: `CLEAN`.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts core/common/build.gradle.kts core/common/src/main/kotlin/app/openflow/core/common/TrimPolicy.kt core/common/src/test/kotlin/app/openflow/core/common/TrimPolicyTest.kt
git commit -m "feat: add :core:common with pure TrimPolicy + tests

Pure Kotlin, no Android deps. Old package untouched for now."
```

---

### Task 4: Backward-compat shim in old package (zero caller edits)

**Files:**
- Modify: `app/src/main/java/app/openflow/runtime/TrimPolicy.kt` (replace 34-line object with 7-line shim)
- Modify: `app/build.gradle.kts` (add one dependency line)
- Test: existing `DocsStaleScanTest` + `QaLoopScanTest` + full `:app:testDebugUnitTest` (no new tests; callers `OpenFlowApp`, `FlowAccessibilityService` compile unchanged)

**Interfaces:**
- Consumes: `app.openflow.core.common.TrimPolicy` from Task 3
- Produces: `app.openflow.runtime.TrimPolicy` still resolves for existing callers (deprecated typealias, zero behavior change)

- [ ] **Step 1: Add module dependency to app**

Modify: `app/build.gradle.kts:149` `dependencies {` block — add first line inside:

```kotlin
implementation(project(":core:common"))
```

- [ ] **Step 2: Replace old object with shim (callers unchanged)**

```kotlin
// app/src/main/java/app/openflow/runtime/TrimPolicy.kt
package app.openflow.runtime

@Deprecated(
    "Moved to app.openflow.core.common.TrimPolicy. Kept as alias so callers need no edits.",
    ReplaceWith("app.openflow.core.common.TrimPolicy"),
)
typealias TrimPolicy = app.openflow.core.common.TrimPolicy
```

- [ ] **Step 3: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest :core:common:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run lint + debug APK (verify-before-done order: unit, lint, debug APK)**

Run: `./gradlew :app:lintDebug :app:assembleDebug`
Expected: BUILD SUCCESSFUL, no new lint errors.

- [ ] **Step 5: Verify no behavior drift (shim resolves, semantics identical)**

Run: `grep -rn "runtime.TrimPolicy\|runtime import.*TrimPolicy\|TrimPolicy\." app/src/main/java/app/openflow/OpenFlowApp.kt app/src/main/java/app/openflow/bubble/FlowAccessibilityService.kt | head -10`
Expected: call sites still compile against the alias; no logic edits in those files (`git diff --stat` shows only the 2 intended files).

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/app/openflow/runtime/TrimPolicy.kt
git commit -m "refactor: point runtime.TrimPolicy at :core:common via alias

Zero caller edits. OpenFlowApp + FlowAccessibilityService untouched."
```

---

## Plan Self-Review

- **Spec coverage:** Spec §4 Phase 0 (core modules, convention plugins) — covered by Tasks 1-3. Spec §5 god-class splits, §7 privacy, M3-A tee — explicitly OUT of this plan; they belong to Plans 02-06 (domain/api/feature/data). This plan is foundation-only and produces a working, testable slice on its own.
- **Placeholder scan:** No TBD/TODO. Every code step has exact file bytes. Every run step has exact command + expected output. No "similar to Task N", no "add appropriate handling".
- **Type consistency:** `TrimPolicy.action/shouldDropIdleStt/shouldReleaseUiCaches/dropIdleEngine/Action/constants` signatures identical in Task 3 implementation and Task 4 alias target. Test in Task 3 calls exactly those signatures.

## Later Plans (not in this file)

- Plan 02: `*-api` abstraction modules (bubble/stt/text/audio contracts, pure Kotlin, no impl).
- Plan 03: `domain-*` use cases for one slice (TrimPolicy consumers as example: bubble visibility).
- Plan 04: `feature-bubble` thin-service split (the 2,024-line service).
- Plan 05: `data-*` modules (dictation/prefs/privacy) + M7 proof.
- Plan 06: M3-A audio tee (`domain-audio` + `feature-audio`) + old-package deletion.
