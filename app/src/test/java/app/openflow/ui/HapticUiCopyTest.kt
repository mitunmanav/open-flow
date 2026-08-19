package app.openflow.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HapticUiCopyTest {
    @Test
    fun copy_covers_presets_and_every_event() {
        assertThat(HapticUiCopy.preset(HapticFeel.OFF)).contains("No buzz")
        assertThat(HapticUiCopy.preset(HapticFeel.CUSTOM)).contains("Set each action")
        HapticFeel.Event.entries.forEach { e ->
            assertThat(HapticUiCopy.event(e).second).isNotEmpty()
        }
        assertThat(HapticUiCopy.PAGE).contains("Custom")
    }
}
