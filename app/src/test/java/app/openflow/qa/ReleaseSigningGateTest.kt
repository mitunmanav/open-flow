package app.openflow.qa

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Release signing gate (LAUNCH_CHECKLIST #release-signing):
 * The `release` signing config must not silently fall back to the public
 * Android debug certificate. Audit H6 — debug-signed AAB/APK allows anyone
 * to impersonate the install; Play upload of an AAB signed with the debug
 * key is the wrong signing story even with Play App Signing re-signing.
 *
 * Source-scan only (no Gradle daemon).
 */
class ReleaseSigningGateTest {

    @Test
    fun release_signing_config_does_not_initWith_debug() {
        val gradle = File(projectRoot(), "app/build.gradle.kts")
        assertThat(gradle.isFile).isTrue()
        val text = gradle.readText()

        // Slice only the `signingConfigs { ... }` block.
        val block = Regex(
            "signingConfigs\\s*\\{[\\s\\S]*?\\n\\s{4}\\}",
        ).find(text)?.value ?: error("signingConfigs block not found")

        // Find the `release` config body (between `create("release")` and its closing `}`).
        val releaseBody = Regex(
            "create\\(\"release\"\\)\\s*\\{([\\s\\S]*?)\\n\\s{4}\\}",
        ).find(block)?.groupValues?.get(1)
            ?: error("release signing config body not found")

        // The release block must NOT initWith(debug). Debug-fallback was the H6 bug.
        assertThat(releaseBody).doesNotContain("""initWith(getByName("debug"))""")
        // Must wire an env keystore path so build fails loudly when missing.
        // The fail-loud guard may live in the signingConfig body OR in a task
        // doFirst block (tasks.matching { assembleRelease|bundleRelease }).
        assertThat(releaseBody).contains("OPENFLOW_KEYSTORE_PATH")
        val hasRequireOrGuard = releaseBody.contains("require(") ||
            (text.contains("OPENFLOW_KEYSTORE_PATH") &&
                text.contains("throw GradleException"))
        assertThat(hasRequireOrGuard).isTrue()
    }

    @Test
    fun localRelease_is_only_used_for_local_sideload() {
        // localRelease keeps initWith(debug) for dev installs. Confirm it is not
        // referenced by any buildType other than a clearly local one.
        val gradle = File(projectRoot(), "app/build.gradle.kts").readText()
        // No buildType outside the file should consume `localRelease`.
        // We assert the literal phrase never appears on a buildType line.
        val offenders = gradle.lineSequence().filter {
            it.contains("localRelease") && it.contains("signingConfig")
        }.toList()
        assertThat(offenders).isEmpty()
    }

    private fun projectRoot(): File {
        val userDir = System.getProperty("user.dir") ?: "."
        var dir = File(userDir).canonicalFile
        repeat(8) {
            if (File(dir, "app/src/main/java/app/openflow/OpenFlowApp.kt").isFile) return dir
            dir = dir.parentFile ?: return@repeat
        }
        error("open-flow root not found from $userDir")
    }
}
