package app.openflow.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HapticFeelTest {

    @Test
    fun off_maps_all_to_none() {
        assertThat(HapticFeel.constantFor("off", HapticFeel.Event.TAP)).isNull()
        assertThat(HapticFeel.constantFor("off", HapticFeel.Event.SAVE)).isNull()
        assertThat(HapticFeel.constantFor("off", HapticFeel.Event.CANCEL)).isNull()
    }

    @Test
    fun light_is_clock_tick_only() {
        assertThat(HapticFeel.constantFor("light", HapticFeel.Event.TAP))
            .isEqualTo(HapticFeel.CLOCK_TICK)
        assertThat(HapticFeel.constantFor("light", HapticFeel.Event.SAVE))
            .isEqualTo(HapticFeel.CLOCK_TICK)
        assertThat(HapticFeel.constantFor("light", HapticFeel.Event.CANCEL))
            .isEqualTo(HapticFeel.CLOCK_TICK)
        assertThat(HapticFeel.CLOCK_TICK).isEqualTo(4)
    }

    @Test
    fun full_maps_tap_save_cancel() {
        assertThat(HapticFeel.constantFor("full", HapticFeel.Event.TAP))
            .isEqualTo(HapticFeel.CONTEXT_CLICK)
        assertThat(HapticFeel.constantFor("full", HapticFeel.Event.SAVE))
            .isEqualTo(HapticFeel.CONFIRM)
        assertThat(HapticFeel.constantFor("full", HapticFeel.Event.CANCEL))
            .isEqualTo(HapticFeel.REJECT)
        assertThat(HapticFeel.CONTEXT_CLICK).isEqualTo(6)
        assertThat(HapticFeel.CONFIRM).isEqualTo(16)
        assertThat(HapticFeel.REJECT).isEqualTo(17)
    }

    @Test
    fun normalize_garbage_is_full() {
        assertThat(HapticFeel.normalize("")).isEqualTo(HapticFeel.FULL)
        assertThat(HapticFeel.normalize("buzz")).isEqualTo(HapticFeel.FULL)
        assertThat(HapticFeel.normalize("LIGHT")).isEqualTo(HapticFeel.LIGHT)
    }
}
