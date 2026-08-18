package app.openflow.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HapticPickTest {
    @Test
    fun map() {
        assertThat(HapticPick.constant(HapticPick.OFF)).isNull()
        assertThat(HapticPick.constant(HapticPick.TICK)).isEqualTo(4)
        assertThat(HapticPick.constant(HapticPick.CLICK)).isEqualTo(6)
        assertThat(HapticPick.constant(HapticPick.CONFIRM)).isEqualTo(16)
        assertThat(HapticPick.constant(HapticPick.REJECT)).isEqualTo(17)
    }

    @Test
    fun garbage_is_click() {
        assertThat(HapticPick.normalize("buzz")).isEqualTo(HapticPick.CLICK)
    }
}
