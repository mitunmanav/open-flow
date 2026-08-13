package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CopyChipTest {

    @Test
    fun show_right_after_commit() {
        assertThat(CopyChip.shouldShow(ageMs = 0L, listening = false)).isTrue()
    }

    @Test
    fun still_show_just_before_six_seconds() {
        assertThat(CopyChip.shouldShow(ageMs = 5_999L, listening = false)).isTrue()
    }

    @Test
    fun hide_after_six_seconds_default() {
        assertThat(CopyChip.shouldShow(ageMs = 6_000L, listening = false)).isFalse()
    }

    @Test
    fun hide_while_listening() {
        assertThat(CopyChip.shouldShow(ageMs = 100L, listening = true)).isFalse()
    }

    @Test
    fun hide_negative_age() {
        assertThat(CopyChip.shouldShow(ageMs = -1L, listening = false)).isFalse()
    }

    @Test
    fun visible_ms_maps_pref() {
        assertThat(CopyChip.visibleMs("3")).isEqualTo(3_000L)
        assertThat(CopyChip.visibleMs("6")).isEqualTo(6_000L)
        assertThat(CopyChip.visibleMs("10")).isEqualTo(10_000L)
    }

    @Test
    fun visible_ms_unknown_defaults_to_six() {
        assertThat(CopyChip.visibleMs("")).isEqualTo(6_000L)
        assertThat(CopyChip.visibleMs("9")).isEqualTo(6_000L)
        assertThat(CopyChip.visibleMs("bogus")).isEqualTo(6_000L)
    }

    @Test
    fun three_second_pref_hides_at_three() {
        val ms = CopyChip.visibleMs("3")
        assertThat(CopyChip.shouldShow(ageMs = 2_999L, listening = false, visibleMs = ms)).isTrue()
        assertThat(CopyChip.shouldShow(ageMs = 3_000L, listening = false, visibleMs = ms)).isFalse()
    }

    @Test
    fun ten_second_pref_still_show_at_six() {
        val ms = CopyChip.visibleMs("10")
        assertThat(CopyChip.shouldShow(ageMs = 6_000L, listening = false, visibleMs = ms)).isTrue()
        assertThat(CopyChip.shouldShow(ageMs = 10_000L, listening = false, visibleMs = ms)).isFalse()
    }
}
