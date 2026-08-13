package app.openflow.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HapticsTest {

    @Test
    fun insert_maps_to_confirm() {
        assertThat(Haptics.constantFor(Haptics.Event.INSERT)).isEqualTo(16)
        assertThat(Haptics.CONFIRM).isEqualTo(16)
    }

    @Test
    fun cancel_maps_to_reject() {
        assertThat(Haptics.constantFor(Haptics.Event.CANCEL)).isEqualTo(17)
        assertThat(Haptics.REJECT).isEqualTo(17)
    }

    @Test
    fun tick_maps_to_clock_tick() {
        assertThat(Haptics.constantFor(Haptics.Event.TICK)).isEqualTo(4)
        assertThat(Haptics.CLOCK_TICK).isEqualTo(4)
    }
}
