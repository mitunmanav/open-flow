import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.openflow.dictation.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ADR-0006 puts the Contract Test suite for every SpeechProvider in `core`, so
    // provider modules depend on it. This is a build-configuration decision, not a
    // source-layout one: `testFixtures` is its own source set with its own
    // configurations, and enabling it changes what `core` publishes to consumers.
    testFixtures {
        enable = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // `api`, not `implementation`: `Flow<SpeechEvent>` is the shape of core's public
    // contract (ADR-0001), so a consumer reading a provider's events must be able to
    // name Flow. ADR-0005 permits `api` only for re-exported contract types — this is
    // the one place it is used today.
    api(libs.kotlinx.coroutines.core)

    // ADR-0006: the Contract Test suite lives in core's fixtures, so what providers
    // assert against is a published artifact, not an internal.
    testFixturesApi(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.junit)

    testImplementation(libs.junit)
}