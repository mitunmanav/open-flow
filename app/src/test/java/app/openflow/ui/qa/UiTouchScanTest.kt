package app.openflow.ui.qa

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/** Source-scan: touch + haptic wiring must stay in the UI. */
class UiTouchScanTest {

    @Test
    fun bubble_xml_has_no_sub48_actions() {
        val layout = File(UiSourceScan.projectRoot(), "app/src/main/res/layout")
        val xml = listOf("flow_bubble.xml", "bubble_close.xml", "bubble_ok.xml")
            .joinToString("\n") { File(layout, it).readText() }
        assertThat(xml).doesNotContain("36dp")
        assertThat(xml).doesNotContain("44dp")
        assertThat(xml).contains("48dp")
    }

    @Test
    fun haptic_map_is_wired_in_shell_and_chips() {
        val shell = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/shell/AppShell.kt"
        ).readText()
        val chip = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/components/OpenChip.kt"
        ).readText()
        assertWithMessage("AppShell must play NAV_TAB haptic")
            .that(shell)
            .contains("UiHapticMap.Event.NAV_TAB")
        assertWithMessage("OpenChip must play CHIP haptic")
            .that(chip)
            .contains("UiHapticMap.Event.CHIP")
    }

    @Test
    fun history_copy_has_a11y_role_label() {
        val src = UiSourceScan.uiKtText()
        assertThat(src).contains("Copy transcript")
        assertThat(src).contains("UiHapticMap.Event.COPY")
    }

    @Test
    fun history_copy_share_are_outlined_buttons() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/history/CopyShareButtons.kt"
        ).readText()
        assertThat(src).contains("history_copy")
        assertThat(src).contains("history_share")
        assertThat(src).contains("ButtonVariant.Outlined")
        assertThat(src).doesNotContain("ButtonVariant.Text")
    }
}
