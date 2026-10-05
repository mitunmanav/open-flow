import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Release signing. ADR-0012.
//
// Nothing below reads a secret out of the repository, and there is no default that
// quietly produces an installable artifact: with no signing config AGP emits an
// UNSIGNED release APK and still reports success. So the build refuses, and the
// escape hatch is a flag nobody types by accident.

/**
 * Where the signing properties are. `-Popenflow.signingProperties=<path>` wins —
 * that is how `.github/workflows/release.yml` points the build at the copy it
 * writes into `RUNNER_TEMP` — otherwise a `signing.properties` at the
 * repository root is used when one exists. That file is gitignored, as is the
 * keystore it names.
 */
val signingPropertiesFile = (
    providers.gradleProperty("openflow.signingProperties").orNull?.let { file(it) }
        ?: rootProject.file("signing.properties").takeIf { it.isFile }
    )

// Named `propsFile` rather than `path`: a Gradle script has `Project.path` in
// scope, and shadowing it with a File is a confusing way to fail.
val signingProperties = signingPropertiesFile?.let { propsFile ->
    Properties().apply {
        try {
            propsFile.inputStream().use { load(it) }
        } catch (e: Exception) {
            throw GradleException(
                "Release signing properties at $propsFile could not be read.", e
            )
        }
    }
}

/** A half-filled properties file is a mistake, not a reason to build unsigned. */
fun Properties.signingKey(name: String): String =
    getProperty(name)?.takeIf { it.isNotBlank() }
        ?: throw GradleException(
            "Release signing properties at $signingPropertiesFile is missing '$name'. " +
                "It must set storeFile, storePassword, keyAlias and keyPassword."
        )

/** A relative `storeFile` resolves beside the properties file, so a keystore and
 *  its properties can sit together outside the repository. */
val signingStoreFile = signingProperties?.let { props ->
    signingPropertiesFile!!.resolveSibling(props.signingKey("storeFile")).also { store ->
        if (!store.isFile) {
            throw GradleException(
                "Keystore ${store.absolutePath}, named by $signingPropertiesFile, " +
                    "does not exist."
            )
        }
    }
}

val signingMissing = signingProperties == null
val allowUnsignedRelease =
    providers.gradleProperty("openflow.allowUnsignedRelease").orNull?.toBoolean() == true

// Release version identity. The tag decides both fields, and the formula is
// deterministic so it can be recomputed by the release checker rather than reported
// back to it. See ADR-0008 and the versioning policy in the map's ticket 50.
//
//   -Popenflow.releaseTag=v0.2.1  ->  versionName 0.2.1, versionCode 2001
//
// A release build without one is refused, including the unsigned build CI uses to
// measure the APK's shape: allowing an unsigned artifact is a separate permission from
// saying which version it is, and a release whose version is a default is a release
// whose identity nobody chose. Debug builds use the development defaults, which are
// never release identity and never acceptance-gate evidence.
private val DEBUG_VERSION_NAME = "0.0.0-dev"
private val DEBUG_VERSION_CODE = 1
private val MAX_VERSION_COMPONENT = 999

data class ReleaseVersion(val name: String, val code: Int) {
    val isRelease: Boolean get() = name != DEBUG_VERSION_NAME
}

/**
 * Parse `vMAJOR.MINOR.PATCH` into the version it implies.
 *
 * Strict on purpose. No leading zeroes, no prerelease suffix, no build metadata, and no
 * component above 999 — a component that could spill into the next component's digit
 * range would let `v0.1000.0` and `v1.0.0` claim the same version code, and a malformed
 * tag fails here rather than being repaired into something nobody agreed to.
 */
fun parseReleaseTag(tag: String): ReleaseVersion {
    val match = Regex("^v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$").find(tag)
        ?: throw GradleException(
            """
            |'${tag}' is not a release tag.
            |
            |Expected vMAJOR.MINOR.PATCH with decimal components, no leading zeroes, no
            |prerelease suffix and no build metadata. A tag is the release's identity
            |here, so one that cannot be read is not something to guess at.
            """.trimMargin()
        )
    val parts = match.groupValues.drop(1).map { it.toInt() }
    parts.forEachIndexed { index, value ->
        val name = listOf("major", "minor", "patch")[index]
        if (value > MAX_VERSION_COMPONENT) {
            throw GradleException(
                "'${tag}' has a ${name} version of ${value}, above the " +
                    "${MAX_VERSION_COMPONENT} bound that keeps version codes distinct."
            )
        }
    }
    if (parts.all { it == 0 }) {
        throw GradleException("'${tag}' is not a release: there is no artifact to identify.")
    }
    val (major, minor, patch) = parts
    return ReleaseVersion(
        name = "$major.$minor.$patch",
        code = major * 1_000_000 + minor * 1_000 + patch,
    )
}

val releaseTag = providers.gradleProperty("openflow.releaseTag").orNull?.trim()
val releaseVersion = releaseTag?.let { parseReleaseTag(it) }

android {
    signingConfigs {
        if (signingProperties != null) {
            create("release") {
                storeFile = signingStoreFile
                storePassword = signingProperties.signingKey("storePassword")
                keyAlias = signingProperties.signingKey("keyAlias")
                keyPassword = signingProperties.signingKey("keyPassword")
            }
        }
    }
    namespace = "dev.openflow.dictation"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.openflow.dictation"
        minSdk = 26
        targetSdk = 36
        // Derived from -Popenflow.releaseTag, never from a literal. A hardcoded pair
        // makes the tag and the artifact's own version independent, so tagging v0.2.0
        // publishes something claiming to be 0.1.0 and two releases can share a code.
        versionCode = releaseVersion?.code ?: DEBUG_VERSION_CODE
        versionName = releaseVersion?.name ?: DEBUG_VERSION_NAME

        // ADR-0010: three ABIs ship, x86 is dropped. This is not release-only —
        // android-test.yml runs the instrumented suite on an x86_64 emulator, so
        // x86_64 has to survive in debug builds too.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    // ADR-0010: native libraries travel compressed and are extracted at install.
    // Measured: 128,850,066 -> 50,977,462 bytes, native libs deflating to 39.1%.
    // This is a download-size decision, not a free win.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            // Left null when there is no signing material, which is what makes the
            // guard below reachable instead of silently signing with the debug key.
            if (signingProperties != null) {
                signingConfig = signingConfigs.getByName("release")
            }

            // Off deliberately: sherpa-onnx ships an EMPTY consumer-rules.pro
            // and its JNI layer resolves classes by name, so an R8 build has no
            // keep rules for it, and with no application code there is nothing
            // to strip anyway. Recorded in docs/adr/0010-release-artifact-shape.md,
            // which is the decision's home — do not re-litigate it per release.
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

/**
 * The guard that makes the above reachable. Every task that produces a release
 * artifact depends on it, so an unsigned release cannot be assembled by accident,
 * and cannot be assembled quietly either.
 */
val requireReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails the build if a release artifact would be produced unsigned."
    doFirst {
        if (signingMissing && !allowUnsignedRelease) {
            throw GradleException(
                """
                |Refusing to build a release artifact without signing material.
                |
                |With no signing config the build does not fall back to the debug key —
                |it signs nothing at all, names the output app-release-unsigned.apk, and
                |reports success. An unsigned APK cannot be installed on a device, so
                |that artifact is a published release nobody can use.
                |
                |Point the build at a properties file setting storeFile, storePassword,
                |keyAlias and keyPassword:
                |  ./gradlew :app:assembleRelease -Popenflow.signingProperties=/path/to/signing.properties
                |or put one at the repository root, which is gitignored. To measure an
                |unsigned build deliberately, say so:
                |  ./gradlew :app:assembleRelease -Popenflow.allowUnsignedRelease=true
                |
                |See docs/adr/0012-release-signing.md.
                """.trimMargin()
            )
        }
    }
}

/**
 * The other half of "a release is an identified artifact". Signing permission and
 * version identity are separate inputs: `-Popenflow.allowUnsignedRelease=true` exists so
 * CI can measure the APK's size, and it must not quietly become permission to ship the
 * development version. So this guard is not about signing at all, and the two are wired
 * to the same task set because an artifact that cannot be identified cannot be gated.
 */
val requireReleaseVersion by tasks.registering {
    group = "verification"
    description = "Fails the build if a release artifact would carry no release version."
    doFirst {
        if (releaseVersion == null) {
            throw GradleException(
                """
                |Refusing to build a release artifact without a release tag.
                |
                |A release build's versionCode and versionName come from the tag, so
                |that an artifact's own identity cannot disagree with the tag it was
                |published under — and so the acceptance gate can match its evidence to
                |a checksum without trusting a number the build reported about itself.
                |
                |  ./gradlew :app:assembleRelease -Popenflow.releaseTag=v0.1.0
                |
                |Debug builds need nothing: they use versionName ${DEBUG_VERSION_NAME}
                |and versionCode ${DEBUG_VERSION_CODE}, which are not release identity.
                |
                |CI's unsigned size check passes a tag too. Measuring the artifact's
                |shape is not permission to publish an unidentified one.
                """.trimMargin()
            )
        }
    }
}

// The three lifecycle tasks that finish a shippable artifact. Not
// installRelease: it publishes nothing, and it fails on its own unsigned.
tasks.matching { it.name in setOf("assembleRelease", "packageRelease", "bundleRelease") }
    .configureEach {
        dependsOn(requireReleaseSigning)
        dependsOn(requireReleaseVersion)
    }

/**
 * The 50 MB release-APK ceiling, ADR-0010. Its real job is narrower than "keep
 * the APK small": fail the build if the ~45 MB ASR model is ever bundled again,
 * which lands near 80 MB. The error names the actual size and the ceiling,
 * because a ceiling nobody can read is not a ceiling. Deliberately asserted
 * against the release build only — the debug APK is ~123 MiB by construction
 * and would be a permanently red gate.
 */
val releaseApkCeilingBytes = 50L * 1024 * 1024

fun assertReleaseApkWithinCeiling() {
    val apkDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
    val apks = apkDir.walkTopDown()
        .filter { it.isFile && it.extension == "apk" }
        .sortedBy { it.name }
        .toList()
    if (apks.isEmpty()) {
        throw GradleException(
            "No release APK found under $apkDir — the size ceiling has nothing to measure."
        )
    }
    for (apk in apks) {
        if (apk.length() > releaseApkCeilingBytes) {
            throw GradleException(
                """
                |Release APK ${apk.name} is ${apk.length()} bytes, over the
                |50 MB ceiling ($releaseApkCeilingBytes bytes).
                |
                |ADR-0010 ships the ~45 MB ASR model as a first-launch download,
                |not a bundled asset. An APK this large means the model was
                |bundled again.
                """.trimMargin()
            )
        }
        logger.lifecycle("Release APK ${apk.name}: ${apk.length()} bytes of a $releaseApkCeilingBytes-byte ceiling.")
    }
}

tasks.matching { it.name in setOf("assembleRelease", "packageRelease") }
    .configureEach { doLast { assertReleaseApkWithinCeiling() } }