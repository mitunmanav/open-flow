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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The benchmark's measurement logic — WER, the statistics, the run manifest,
    // the threshold verdict, the report — is pure Kotlin with no Android and no
    // sherpa-onnx in it. It is wired into the unit-test source set so it compiles
    // against the module's tests; ticket 40's device driver will add the
    // `androidTest` wiring it needs, because the driver and the CI gate must run
    // the same code. It reaches no shipped variant: putting it in `main/` would
    // ship a benchmark instrument inside the release AAR.
    sourceSets {
        getByName("test") { java.srcDir("src/benchmark/kotlin") }
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

    // This module's CI gate: the ModelStore suite — fetch, verify, extract, hand
    // over a path (ticket 46) — with the archive extractor and the V1 model table
    // beside it.
    testImplementation(libs.junit)
}