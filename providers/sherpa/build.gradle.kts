import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.openflow.dictation.providers.sherpa"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        // The benchmark harness's entry point (ticket 40) is an instrumentation
        // test, so it needs a runner. `androidx.test.runner.AndroidJUnitRunner` is
        // the stock one; the harness adds no custom runner because it needs no
        // custom lifecycle.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The benchmark's measurement logic — WER, the statistics, the run manifest,
    // the threshold verdict, the report — is pure Kotlin with no Android and no
    // sherpa-onnx in it, and it must be compiled into BOTH the unit-test APK and
    // the instrumentation APK so the device driver and the CI gate are running the
    // same code. That is what this directory is: a source set wired into both,
    // which is also why it reaches no shipped variant. Putting it in `main/`
    // instead would put a benchmark instrument in the release AAR; duplicating it
    // would let the two copies disagree, which is the failure this repo has been
    // bitten by twice.
    sourceSets {
        getByName("test") { java.srcDir("src/benchmark/kotlin") }
        getByName("androidTest") { java.srcDir("src/benchmark/kotlin") }
    }

    testOptions {
        unitTests {
            // So a unit test can read a document in the repository. `MatrixDriftTest`
            // compares the candidate matrix in code against the one written in
            // `docs/providers/model-selection.md`, and the default working directory
            // for an AGP unit test is the module directory, where that document is
            // not. Set here rather than worked around in the test, so the reason is
            // written down where someone changing it will see it.
            all { it.workingDir = rootDir }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // ADR-0005: a provider depends only on core's public contract types. `app` also
    // depends on `core` directly, so the contract does not need re-exporting here.
    implementation(project(":core"))

    // The prebuilt AAR. It ships native libraries for four ABIs and needs no NDK:
    // JitPack mirrors the AAR that sherpa-onnx's own release pipeline produced.
    // Its Kotlin metadata is 1.7.x, which the Kotlin 2.0.21 compiler reads.
    implementation(libs.sherpa.onnx)

    // The ModelStore's tar.bz2 extraction (ticket 46). `implementation`, not
    // `api`: the module's public signatures name only File, String and Map, so
    // commons-compress never reaches this module's consumers.
    implementation(libs.commons.compress)

    // CI's gate for this module. `ModelMatrixTest` in particular reads
    // `docs/providers/model-selection.md` and fails when the candidate matrix in
    // code has drifted from the one the plan states, so this is a real check and
    // not a formality.
    testImplementation(libs.junit)

    // The benchmark harness's device driver. `androidTestImplementation`, never
    // `implementation`: the instrument must not reach the release AAR.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}