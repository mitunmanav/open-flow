package app.openflow.bubble

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/** Settings must iterate catalog — no duplicate hardcoded shape/tint lists. */
class BubbleCatalogWireScanTest {
    @Test
    fun bubble_settings_uses_shape_and_tint_catalogs() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/settings/BubbleSettings.kt",
        ).readText()
        assertThat(src).contains("BubbleShapeCatalog.ALL")
        assertThat(src).contains("BubbleTint.ALL")
        assertThat(src).doesNotContain("\"pill\" to \"Pill\"")
    }

    @Test
    fun prefs_normalize_delegates_to_catalog() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/prefs/FlowPrefs.kt",
        ).readText()
        assertThat(src).contains("BubbleShapeCatalog.normalize")
    }
}
