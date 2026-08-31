package app.openflow.ui.qa

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/** Paste-import piles must stay gone from leaf UI files. */
class UiImportHygieneScanTest {
    private val leafFiles = listOf(
        "dictionary/DictionaryTab.kt",
        "snippets/SnippetsTab.kt",
        "history/HistoryScreen.kt",
        "history/HistoryRaw.kt",
        "dictionary/PairImportBlock.kt",
    )

    private val bannedInLeaves = listOf(
        "import app.openflow.ui.walkthrough.WalkthroughPager",
        "import app.openflow.ui.walkthrough.WalkthroughPolicy",
        "import app.openflow.ui.engine.EngineSettingsScreen",
        "import app.openflow.ui.setup.SetupWizard",
        "import app.openflow.ui.shell.AppShell",
        "import app.openflow.ui.home.HomeFeed",
        "import app.openflow.ui.haptics.HapticsSettings",
        "import app.openflow.ui.insights.InsightsScreen",
        "import app.openflow.ui.style.StyleHubScreen",
    )

    @Test
    fun leaf_ui_files_do_not_paste_main_activity_imports() {
        val ui = File(UiSourceScan.projectRoot(), "app/src/main/java/app/openflow/ui")
        leafFiles.forEach { rel ->
            val src = File(ui, rel).readText()
            bannedInLeaves.forEach { ban ->
                assertWithMessage("$rel must not contain $ban").that(src).doesNotContain(ban)
            }
            val importCount = src.lineSequence().count { it.startsWith("import ") }
            assertWithMessage("$rel import count").that(importCount).isAtMost(80)
        }
    }

    @Test
    fun history_raw_only_imports_clipboard_path() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/history/HistoryRaw.kt",
        ).readText()
        assertThat(src).contains("FlowAccessibilityService")
        assertThat(src.lineSequence().count { it.startsWith("import ") }).isAtMost(12)
    }
}
