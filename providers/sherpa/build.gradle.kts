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

    // Unit tests for the ModelStore (ticket 46): the download/verify/extract
    // path is exercised against a fake downloader and real tiny archives.
    testImplementation(libs.junit)
}