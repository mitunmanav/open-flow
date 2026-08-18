package app.openflow.prefs

import app.openflow.ui.HapticFeel
import app.openflow.ui.HapticPick
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlowPrefsHapticPickTest {
    @Test
    fun defaults() {
        val p = FlowPrefs(MemoryPrefsStore())
        assertThat(p.hapticPick(HapticFeel.Event.TAP)).isEqualTo(HapticPick.CLICK)
        assertThat(p.hapticPick(HapticFeel.Event.SAVE)).isEqualTo(HapticPick.CONFIRM)
        assertThat(p.hapticPick(HapticFeel.Event.CANCEL)).isEqualTo(HapticPick.REJECT)
        assertThat(p.hapticPick(HapticFeel.Event.ERROR)).isEqualTo(HapticPick.REJECT)
        assertThat(p.hapticPick(HapticFeel.Event.LISTEN)).isEqualTo(HapticPick.TICK)
    }

    @Test
    fun old_off_migrates() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.hapticFeel = HapticFeel.OFF
        assertThat(p.hapticPick(HapticFeel.Event.TAP)).isEqualTo(HapticPick.OFF)
    }

    @Test
    fun reset_restores_defaults() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.setHapticPick(HapticFeel.Event.TAP, HapticPick.OFF)
        p.resetHaptics()
        assertThat(p.hapticPick(HapticFeel.Event.TAP)).isEqualTo(HapticPick.CLICK)
    }
}
