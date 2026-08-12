package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CopyChipTest {

    @Test
    fun show_right_after_commit() {
        assertThat(CopyChip.shouldShow(ageMs = 0L, listening = false)).isTrue()
    }

    @Test
    fun hide_after_ten_seconds() {
        assertThat(CopyChip.shouldShow(ageMs = 10_000L, listening = false)).isFalse()
    }

    @Test
    fun hide_while_listening() {
        assertThat(CopyChip.shouldShow(ageMs = 100L, listening = true)).isFalse()
    }

    @Test
    fun hide_negative_age() {
        assertThat(CopyChip.shouldShow(ageMs = -1L, listening = false)).isFalse()
    }
}
