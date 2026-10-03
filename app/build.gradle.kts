import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.openflow.dictation"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.openflow.dictation"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            // Off deliberately for now. sherpa-onnx ships an EMPTY consumer-rules.pro
            // and its JNI layer resolves classes by name, so an R8 build has no keep
            // rules for it. Turning minification on is a decision with its own
            // ticket, not a default to drift into.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lint {
        // A red gate nobody trusts is worse than a permissive one. Errors fail;
        // warnings are reported and left for review.
        abortOnError = true
        warningsAsErrors = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // ADR-0005: app depends on core and on providers. Wiring is manual constructor
    // injection — there is no DI framework in V1.
    implementation(project(":core"))
    implementation(project(":providers:sherpa"))
}