package app.openflow.ui.haptics

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class HapticSettingsScanTest {
    @Test
    fun settings_use_presets_and_custom_panel() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/haptics/HapticsSettings.kt"
        ).readText()
        assertThat(src).contains("HapticUiCopy.PAGE")
        assertThat(src).contains("HapticPresetSelector")
        assertThat(src).contains("HapticCustomPanel")
        assertThat(src).contains("haptics_test")
        assertThat(src).contains("haptics_reset")
        val presets = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/haptics/HapticPresetSelector.kt"
        ).readText()
        assertThat(presets).contains("haptics_preset_")
        assertThat(presets).contains("HapticFeel.CUSTOM")
    }
}
