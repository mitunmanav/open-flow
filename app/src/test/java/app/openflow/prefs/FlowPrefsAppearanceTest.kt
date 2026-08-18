package app.openflow.prefs

import app.openflow.ui.theme.AppearancePalette
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlowPrefsAppearanceTest {
    @Test
    fun empty_store_is_light_factory() {
        val p = FlowPrefs(MemoryPrefsStore())
        assertThat(p.palette()).isEqualTo(AppearancePalette.factory(false))
    }

    @Test
    fun set_bg_hex_updates_palette() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.colorBg = "#FF0000"
        assertThat(p.palette().backgroundArgb).isEqualTo(0xFFFF0000.toInt())
    }

    @Test
    fun reset_clears_custom() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.colorBg = "#FF0000"
        p.resetAppearanceColors()
        assertThat(p.colorBg).isEmpty()
        assertThat(p.palette().backgroundArgb)
            .isEqualTo(AppearancePalette.factory(false).backgroundArgb)
    }
}
