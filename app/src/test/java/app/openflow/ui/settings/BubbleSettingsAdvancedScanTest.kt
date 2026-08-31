package app.openflow.ui.settings

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class BubbleSettingsAdvancedScanTest {
    @Test
    fun essentials_gate_advanced_behind_toggle() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/settings/BubbleSettings.kt",
        ).readText()
        assertThat(src).contains("bubble_advanced")
        assertThat(src).contains("if (showAdvanced)")
        assertThat(src).contains("BubbleShapeCatalog.ALL")
        assertThat(src).contains("BubbleTint.ALL")
    }
}
