package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleTouchTest {

    @Test
    fun action_and_chip_meet_48dp_min() {
        assertThat(BubbleTouch.ACTION_DP).isAtLeast(48)
        assertThat(BubbleTouch.CHIP_MIN_DP).isAtLeast(48)
    }

    @Test
    fun listen_bar_fits_action_plus_pad() {
        assertThat(BubbleTouch.LISTEN_BAR_DP)
            .isAtLeast(BubbleTouch.ACTION_DP + BubbleTouch.PAD_V_DP * 2)
    }

    @Test
    fun listen_row_is_three_discs_plus_gaps() {
        assertThat(BubbleTouch.GAP_DP).isEqualTo(8)
        assertThat(BubbleTouch.listenWidthDp()).isEqualTo(48 * 3 + 8 * 2)
        assertThat(BubbleTouch.listenWidthDp(cancel = false)).isEqualTo(48 * 2 + 8)
        assertThat(BubbleTouch.listenWidthDp(done = false)).isEqualTo(48 * 2 + 8)
        assertThat(BubbleTouch.listenWidthDp(cancel = false, done = false)).isEqualTo(48)
    }

    @Test
    fun overlay_listen_uses_disc_height() {
        val (_, h) = BubbleGeometry.overlaySizePx(listening = true, density = 2f)
        assertThat(h).isEqualTo((BubbleTouch.ACTION_DP * 2f).toInt())
    }

    @Test
    fun overlay_chips_uses_listen_bar() {
        val idle = BubbleGeometry.overlaySizePx(listening = false, density = 2f, shape = "pill")
        val chips = BubbleGeometry.overlaySizePx(
            listening = false,
            density = 2f,
            shape = "pill",
            chips = true,
        )
        assertThat(chips.second).isEqualTo((BubbleTouch.LISTEN_BAR_DP * 2f).toInt())
        assertThat(chips.second).isGreaterThan(idle.second)
    }
}
