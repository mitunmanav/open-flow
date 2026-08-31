package app.openflow.ui.insights

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class InsightsDensityScanTest {
    @Test
    fun usage_has_more_toggle_and_primary_four() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/insights/InsightsScreen.kt",
        ).readText()
        assertThat(src).contains("insights_more")
        assertThat(src).contains("Tile(\"Words\"")
        assertThat(src).contains("Tile(\"Sessions\"")
        assertThat(src).contains("Tile(\"WPM\"")
        assertThat(src).contains("Tile(\"Streak\"")
        assertThat(src).contains("if (showMore)")
        assertThat(src).contains("insights_heatmap")
        assertThat(src).contains("insights_by_app")
    }
}
