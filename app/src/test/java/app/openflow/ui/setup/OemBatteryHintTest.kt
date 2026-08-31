package app.openflow.ui.setup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OemBatteryHintTest {

    @Test
    fun samsung_shows_samsung_only() {
        val h = OemBatteryHint.hint("samsung")
        assertThat(h).contains("Samsung")
        assertThat(h.lowercase()).doesNotContain("xiaomi")
        assertThat(h).contains("Unrestricted")
        assertThat(h).contains("Skip")
    }

    @Test
    fun xiaomi_family_shows_xiaomi() {
        listOf("Xiaomi", "xiaomi", "Redmi", "POCO", "poco").forEach { m ->
            val h = OemBatteryHint.hint(m)
            assertThat(h).contains("Xiaomi")
            assertThat(h).contains("Autostart")
        }
    }

    @Test
    fun huawei_honor_show_huawei() {
        assertThat(OemBatteryHint.hint("HUAWEI")).contains("Huawei")
        assertThat(OemBatteryHint.hint("HUAWEI")).contains("Manage manually")
        assertThat(OemBatteryHint.hint("HONOR")).contains("Huawei")
    }

    @Test
    fun oppo_realme_oneplus_share_hint() {
        listOf("OPPO", "Realme", "OnePlus").forEach { m ->
            val h = OemBatteryHint.hint(m)
            assertThat(h).contains("OnePlus")
            assertThat(h).contains("Autostart")
        }
    }

    @Test
    fun vivo_shows_vivo() {
        assertThat(OemBatteryHint.hint("vivo")).contains("Vivo")
        assertThat(OemBatteryHint.hint("vivo")).contains("High background")
    }

    @Test
    fun generic_shows_tip_not_samsung_wall() {
        val g = OemBatteryHint.hint("google")
        assertThat(g).startsWith("Tip:")
        assertThat(g).doesNotContain("Samsung: Settings → Apps → Open Flow → Battery → Unrestricted. Xiaomi:")
        assertThat(g).contains("Unrestricted")
        // Pixel, Motorola, Nothing handle via generic Tip without OEM wall
        assertThat(OemBatteryHint.hint(null)).contains("Unrestricted")
        assertThat(OemBatteryHint.hint("")).contains("Unrestricted")
        assertThat(OemBatteryHint.hint("motorola")).startsWith("Tip:")
    }

    @Test
    fun all_hints_are_skip_friendly_and_short() {
        listOf("samsung", "xiaomi", "huawei", "oppo", "vivo", "google", null).forEach { m ->
            val h = OemBatteryHint.hint(m)
            assertThat(h).contains("Skip")
            // keep hint readable — not a wall, single sentence-ish
            assertThat(h.length).isLessThan(180)
        }
    }
}
