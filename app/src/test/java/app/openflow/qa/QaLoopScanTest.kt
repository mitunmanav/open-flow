package app.openflow.qa

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/** Pin the device QA loop files so the setup cannot rot silently. */
class QaLoopScanTest {

    private val root = UiSourceScan.projectRoot()

    @Test
    fun qa_scripts_exist() {
        listOf(
            "scripts/qa/lib.sh",
            "scripts/qa/wrap-adb.sh",
            "scripts/qa/adb-bridge.sh",
            "scripts/qa/adb-bridge.py",
            "scripts/qa/emu-up.sh",
            "scripts/qa/prep.sh",
            "scripts/qa/crash-scan.sh",
            "scripts/qa/gate.sh",
        ).forEach { rel ->
            assertWithMessage("missing $rel")
                .that(File(root, rel).isFile)
                .isTrue()
        }
    }

    @Test
    fun testing_doc_and_agents_ban_wsl_avd() {
        val testing = File(root, "docs/testing.md").readText()
        val agents = File(root, "AGENTS.md").readText()
        assertThat(testing).contains("of_win")
        assertThat(testing).contains("scripts/qa/gate.sh")
        assertThat(testing).contains("wrap-adb")
        assertThat(agents).contains("android emulator start of_test")
        assertThat(agents).contains("scripts/qa/gate.sh")
    }

    @Test
    fun emu_up_does_not_kill_qemu() {
        val emu = File(root, "scripts/qa/emu-up.sh").readText()
        assertThat(emu).doesNotContain("pkill")
        assertThat(emu).contains("-gpu host")
        assertThat(emu).doesNotContain("-no-snapshot-load")
    }

    @Test
    fun espresso_pinned_for_api37() {
        val gradle = File(root, "app/build.gradle.kts").readText()
        assertThat(gradle).contains("espresso-core:3.7.0")
    }

    @Test
    fun native_is_16kb_ready() {
        val gradle = File(root, "app/build.gradle.kts").readText()
        val cmake = File(root, "app/src/main/cpp/CMakeLists.txt").readText()
        assertThat(gradle).contains("ndkVersion = \"28.2.13676358\"")
        assertThat(gradle).contains("ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
        assertThat(gradle).contains("graphics-path:1.1.0")
        assertThat(cmake).contains("max-page-size=16384")
    }
}
