package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleVisualDriverTest {

    @Test
    fun listening_pins_master_scale() {
        val s = BubbleVisualDriver.effectiveScale(
            scale = 0.9f, shrinkIdle = true, shrinkDot = true,
            shrinkSearch = true, listening = true, searchFieldFocused = true,
        )
        assertThat(s).isEqualTo(0.9f)
    }

    @Test
    fun idle_shrinks_by_mode() {
        fun scale(dot: Boolean) = BubbleVisualDriver.effectiveScale(
            scale = 1f, shrinkIdle = true, shrinkDot = dot,
            shrinkSearch = false, listening = false, searchFieldFocused = false,
        )
        assertThat(scale(dot = true)).isEqualTo(0.55f)
        assertThat(scale(dot = false)).isEqualTo(0.75f)
    }

    @Test
    fun master_off_keeps_full_size() {
        val s = BubbleVisualDriver.effectiveScale(
            scale = 0.9f, shrinkIdle = false, shrinkDot = true,
            shrinkSearch = true, listening = false, searchFieldFocused = true,
        )
        assertThat(s).isEqualTo(0.9f)
    }

    @Test
    fun emphasis_alpha_full_on_field_dimmed_off() {
        assertThat(BubbleVisualDriver.emphasisAlpha(1f, hasField = true)).isEqualTo(1f)
        assertThat(BubbleVisualDriver.emphasisAlpha(1f, hasField = false)).isEqualTo(0.8f)
        assertThat(BubbleVisualDriver.emphasisAlpha(null, hasField = true)).isEqualTo(0.95f)
        assertThat(BubbleVisualDriver.emphasisAlpha(null, hasField = false)).isWithin(1e-6f).of(0.68f)
    }
}
