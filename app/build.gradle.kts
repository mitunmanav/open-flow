import org.gradle.api.GradleException

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "app.openflow"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.openflow"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "0.1.9"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                )
            }
        }
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
        create("localRelease") {
            initWith(getByName("debug"))
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
        // Release: real upload keystore only. NO fallback to debug key.
        // Env: OPENFLOW_KEYSTORE_PATH / _PASS / _ALIAS / _KEY_PASS
        // gradle.properties: openflow.keystore.path/pass/alias/pass
        // Missing → build fails loudly (better than shipping a debug-signed artifact).
        create("release") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            val ksPath = findProperty("openflow.keystore.path") as String?
                ?: System.getenv("OPENFLOW_KEYSTORE_PATH")
            val ksPass = findProperty("openflow.keystore.pass") as String?
                ?: System.getenv("OPENFLOW_KEYSTORE_PASS")
            val alias = findProperty("openflow.key.alias") as String?
                ?: System.getenv("OPENFLOW_KEY_ALIAS")
            val keyPass = findProperty("openflow.key.pass") as String?
                ?: System.getenv("OPENFLOW_KEY_PASS") ?: ksPass
            if (ksPath != null && file(ksPath).exists() &&
                ksPass != null && alias != null
            ) {
                storeFile = file(ksPath)
                storePassword = ksPass
                keyAlias = alias
                keyPassword = keyPass
            }
            // Missing keystore → signingConfig is incomplete. The `release` buildType
            // below asserts and fails the build at variant-config time only.
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildToolsVersion = "36.0.0"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        animationsDisabled = true
    }

    // Release-signing fail-loud guard: refuse to ship a debug-signed release artifact.
// Runs before the AGP signing check so the user sees a clear message instead of
// the cryptic "SigningConfig is missing required property storeFile".
    fun guardReleaseSigning() {
        val sc = android.signingConfigs.findByName("release")
        if (sc == null || sc.storeFile == null) {
            throw GradleException(
                "OPENFLOW_KEYSTORE_PATH (or openflow.keystore.path) is required " +
                    "to build a release artifact. Debug-signed releases ship a " +
                    "publicly-known cert; the build refuses to ship that."
            )
        }
    }
    tasks.matching {
        it.name in setOf(
            "assembleRelease",
            "bundleRelease",
            "packageRelease",
            "signingConfigWriterRelease",
        )
    }.configureEach { doFirst { guardReleaseSigning() } }
    lint {
        abortOnError = false
    }
    ndkVersion = "28.2.13676358"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Compose 1.7.5 pulls 1.0.1 (4 KB ELF). 1.1.0 is 16 KB aligned.
    // https://developer.android.com/jetpack/androidx/releases/graphics#graphics-path-1.1.0
    implementation("androidx.graphics:graphics-path:1.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    // 3.7.0: getSystemService, not InputManager.getInstance (gone on API 36.1+/37).
    // https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.truth:truth:1.4.4")

    // Pure JVM unit tests only — no Robolectric (avoids big jar download).
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.google.truth:truth:1.4.4")
}
