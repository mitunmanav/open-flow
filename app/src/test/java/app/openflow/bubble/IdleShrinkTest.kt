package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IdleShrinkTest {

    @Test
    fun compact_after_five_seconds_idle() {
        assertThat(IdleShrink.shouldCompact(5_000L, listening = false, dragging = false)).isTrue()
    }

    @Test
    fun not_compact_before_threshold() {
        assertThat(IdleShrink.shouldCompact(4_999L, listening = false, dragging = false)).isFalse()
    }

    @Test
    fun not_compact_while_listening() {
        assertThat(IdleShrink.shouldCompact(10_000L, listening = true, dragging = false)).isFalse()
    }

    @Test
    fun not_compact_while_dragging() {
        assertThat(IdleShrink.shouldCompact(10_000L, listening = false, dragging = true)).isFalse()
    }
}
