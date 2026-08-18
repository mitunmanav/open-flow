package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleSnoozePolicyTest {
    @Test
    fun blocked_when_ime_open() {
        assertThat(
            BubbleSnoozePolicy.canSnooze(
                imeVisible = true,
                listening = false,
                repairShowing = false,
            )
        ).isFalse()
    }

    @Test
    fun allowed_idle_ime_down() {
        assertThat(
            BubbleSnoozePolicy.canSnooze(
                imeVisible = false,
                listening = false,
                repairShowing = false,
            )
        ).isTrue()
    }

    @Test
    fun blocked_while_listening() {
        assertThat(
            BubbleSnoozePolicy.canSnooze(
                imeVisible = false,
                listening = true,
                repairShowing = false,
            )
        ).isFalse()
    }
}
