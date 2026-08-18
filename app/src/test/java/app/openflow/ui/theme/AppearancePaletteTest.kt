package app.openflow.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppearancePaletteTest {
    @Test
    fun light_factory_is_brutal_cream() {
        val p = AppearancePalette.factory(dark = false)
        assertThat(p.backgroundArgb).isEqualTo(0xFFF4F1EA.toInt())
        assertThat(p.textArgb).isEqualTo(0xFF1A1A1A.toInt())
        assertThat(p.bubbleIdleArgb).isEqualTo(0xFF1A1A18.toInt())
        assertThat(p.bubbleTextArgb).isEqualTo(0xFFF4EFE6.toInt())
    }

    @Test
    fun empty_hex_keeps_factory() {
        val base = AppearancePalette.factory(false)
        val p = AppearancePalette.overlay(
            dark = false, bg = "", cards = "", text = "", accent = "",
            border = "", bubbleIdle = "", bubbleListen = "", bubbleText = ""
        )
        assertThat(p).isEqualTo(base)
    }

    @Test
    fun one_custom_slot_overrides() {
        val p = AppearancePalette.overlay(
            dark = false, bg = "#FF0000", cards = "", text = "", accent = "",
            border = "", bubbleIdle = "", bubbleListen = "", bubbleText = ""
        )
        assertThat(p.backgroundArgb).isEqualTo(0xFFFF0000.toInt())
        assertThat(p.textArgb).isEqualTo(AppearancePalette.factory(false).textArgb)
    }

    @Test
    fun bad_hex_uses_factory_slot() {
        val p = AppearancePalette.overlay(
            dark = false, bg = "zzz", cards = "", text = "", accent = "",
            border = "", bubbleIdle = "", bubbleListen = "", bubbleText = ""
        )
        assertThat(p.backgroundArgb).isEqualTo(AppearancePalette.factory(false).backgroundArgb)
    }

    @Test
    fun reset_is_light_factory() {
        assertThat(AppearancePalette.reset()).isEqualTo(AppearancePalette.factory(false))
    }
}
