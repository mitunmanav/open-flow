package app.openflow.ui.qa

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** First frame must be brutal cream. No white/black flash. No M3 purple chrome. */
class WindowChromeTest {

    @Test
    fun cream_named_in_colors() {
        val colors = UiSourceScan.valuesFile("colors.xml").readText()
        assertThat(colors).contains("brutal_cream")
        assertThat(colors).contains("#F4F1EA")
        assertThat(colors).contains("brutal_charcoal")
        assertThat(colors).contains("#1A1A1A")
    }

    @Test
    fun window_first_frame_is_cream() {
        val theme = UiSourceScan.valuesFile("themes.xml").readText()
        assertThat(theme).contains("Theme.OpenFlow")
        assertThat(theme).containsMatch(
            """<item name="android:windowBackground">\s*@color/brutal_cream\s*</item>"""
        )
        assertThat(theme).containsMatch(
            """<item name="android:statusBarColor">\s*@color/brutal_cream\s*</item>"""
        )
        assertThat(theme).containsMatch(
            """<item name="android:navigationBarColor">\s*@color/brutal_(cream|charcoal)\s*</item>"""
        )
        assertThat(theme).containsMatch(
            """<item name="android:windowLightStatusBar">\s*true\s*</item>"""
        )
        assertThat(theme).doesNotContain("@android:color/white")
        assertThat(theme).doesNotContain("@android:color/black")
        assertThat(theme).doesNotContain("#FFFFFF")
        assertThat(theme).doesNotContain("#000000")
    }

    @Test
    fun splash_and_primary_stay_brutal() {
        // Theme is split across values/v27/v31 qualifiers; every config must stay brutal.
        val base = UiSourceScan.valuesFile("themes.xml").readText()
        val v27 = UiSourceScan.valuesFile("themes.xml", qualifier = "values-v27").readText()
        val v31 = UiSourceScan.valuesFile("themes.xml", qualifier = "values-v31").readText()
        for (theme in listOf(base, v27, v31)) {
            assertThat(theme).doesNotContain("@color/openflow_primary")
            assertThat(theme).doesNotContain("@color/openflow_indigo")
            assertThat(theme).doesNotContain("#4F46E5")
            assertThat(theme).doesNotContain("#6366F1")
        }
        // Splash background only exists where the attribute exists (API 31+).
        assertThat(base).doesNotContain("windowSplashScreenBackground")
        assertThat(v27).doesNotContain("windowSplashScreenBackground")
        assertThat(v31).containsMatch(
            """<item name="android:windowSplashScreenBackground">\s*@color/brutal_cream\s*</item>"""
        )
    }
}
