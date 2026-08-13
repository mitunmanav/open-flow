package app.openflow.prefs

import app.openflow.ui.HapticFeel
import app.openflow.ui.theme.BubbleTint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlowPrefsFeelFixTest {

    @Test
    fun bubbleTint_defaults_charcoal() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.bubbleTint).isEqualTo(BubbleTint.CHARCOAL)
    }

    @Test
    fun bubbleTint_normalizes() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        prefs.bubbleTint = "INK"
        assertThat(prefs.bubbleTint).isEqualTo(BubbleTint.INK)
        prefs.bubbleTint = "nope"
        assertThat(prefs.bubbleTint).isEqualTo(BubbleTint.CHARCOAL)
    }

    @Test
    fun hapticFeel_defaults_full_and_maps_off() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.hapticFeel).isEqualTo(HapticFeel.FULL)
        assertThat(prefs.bubbleHaptics).isTrue()
        prefs.hapticFeel = "off"
        assertThat(prefs.hapticFeel).isEqualTo(HapticFeel.OFF)
        assertThat(prefs.bubbleHaptics).isFalse()
        prefs.hapticFeel = "light"
        assertThat(prefs.hapticFeel).isEqualTo(HapticFeel.LIGHT)
        assertThat(prefs.bubbleHaptics).isTrue()
    }

    @Test
    fun copyChipSec_3_6_10() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.copyChipSec).isEqualTo(6)
        prefs.copyChipSec = 3
        assertThat(prefs.copyChipSec).isEqualTo(3)
        prefs.copyChipSec = 10
        assertThat(prefs.copyChipSec).isEqualTo(10)
        prefs.copyChipSec = 9
        assertThat(prefs.copyChipSec).isEqualTo(6)
    }
}
