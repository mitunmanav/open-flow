package app.openflow.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UiHapticMapTest {

    @Test
    fun nav_tab_is_clock_tick() {
        assertThat(UiHapticMap.constant(UiHapticMap.Event.NAV_TAB)).isEqualTo(4)
    }

    @Test
    fun chip_is_context_click() {
        assertThat(UiHapticMap.constant(UiHapticMap.Event.CHIP)).isEqualTo(6)
    }

    @Test
    fun slider_is_clock_tick() {
        assertThat(UiHapticMap.constant(UiHapticMap.Event.SLIDER)).isEqualTo(4)
    }

    @Test
    fun copy_is_confirm() {
        assertThat(UiHapticMap.constant(UiHapticMap.Event.COPY)).isEqualTo(16)
    }

    @Test
    fun error_is_reject() {
        assertThat(UiHapticMap.constant(UiHapticMap.Event.ERROR)).isEqualTo(17)
    }

    @Test
    fun events_are_five() {
        assertThat(UiHapticMap.Event.entries.toList()).containsExactly(
            UiHapticMap.Event.NAV_TAB,
            UiHapticMap.Event.CHIP,
            UiHapticMap.Event.SLIDER,
            UiHapticMap.Event.COPY,
            UiHapticMap.Event.ERROR,
        ).inOrder()
    }
}
