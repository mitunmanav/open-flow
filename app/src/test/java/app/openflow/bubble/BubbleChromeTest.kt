package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleChromeTest {

    @Test
    fun idle_fill_is_charcoal_not_zinc() {
        // Brutal charcoal #1A1A1A — not Material zinc #18181B soft pill.
        assertThat(BubbleChrome.IDLE_FILL).isEqualTo(0xFF1A1A1A.toInt())
    }

    @Test
    fun idle_stroke_is_cream_high_contrast() {
        assertThat(BubbleChrome.IDLE_STROKE).isEqualTo(0xFFF4F1EA.toInt())
    }

    @Test
    fun listen_stroke_is_cream_not_indigo() {
        // No purple/indigo accent — stays monochrome brutal.
        assertThat(BubbleChrome.LISTEN_STROKE).isEqualTo(0xFFF4F1EA.toInt())
        assertThat(BubbleChrome.LISTEN_STROKE).isNotEqualTo(0xFF6366F1.toInt())
    }

    @Test
    fun icon_is_cream_on_charcoal() {
        assertThat(BubbleChrome.ICON).isEqualTo(0xFFF4F1EA.toInt())
    }

    @Test
    fun square_corner_is_minimal_hard() {
        // density 2 → 2dp * 2 = 4px hard edge, not 16dp soft squircle.
        assertThat(BubbleChrome.cornerPx("square", density = 2f)).isEqualTo(4f)
        assertThat(BubbleChrome.cornerPx("circle", density = 2f)).isAtLeast(1000f)
    }

    @Test
    fun listen_bar_uses_hard_not_pill() {
        assertThat(BubbleChrome.cornerPx("listen", density = 2f)).isEqualTo(4f)
    }

    @Test
    fun roundness_softens_square_and_pill() {
        assertThat(BubbleChrome.cornerPx("square", 2f, BubbleChrome.ROUND_SOFT)).isEqualTo(16f)
        assertThat(BubbleChrome.cornerPx("square", 2f, BubbleChrome.ROUND_ROUND)).isEqualTo(32f)
        assertThat(BubbleChrome.cornerPx("pill", 2f, BubbleChrome.ROUND_HARD)).isEqualTo(24f)
        assertThat(BubbleChrome.cornerPx("pill", 2f, BubbleChrome.ROUND_ROUND)).isEqualTo(48f)
        assertThat(BubbleChrome.normalizeRoundness("weird")).isEqualTo(BubbleChrome.ROUND_HARD)
    }

    @Test
    fun stroke_width_is_bold() {
        assertThat(BubbleChrome.strokePx(density = 2f)).isEqualTo(4) // 2dp
    }
}
