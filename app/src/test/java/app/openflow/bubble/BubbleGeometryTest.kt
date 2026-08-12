package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleGeometryTest {

    @Test
    fun snap_prefers_right_when_closer_to_end() {
        // Gravity.END: x=0 is right edge; larger x → left
        assertThat(BubbleGeometry.snapOffsetFromEnd(40, screenWidthPx = 1080, bubbleWidthPx = 120))
            .isEqualTo(32)
    }

    @Test
    fun snap_prefers_left_when_past_mid() {
        assertThat(BubbleGeometry.snapOffsetFromEnd(800, screenWidthPx = 1080, bubbleWidthPx = 120))
            .isEqualTo(1080 - 120 - 32)
    }

    @Test
    fun clampVerticalOffset_bounds_properly() {
        // Screen height 2400, bubble height 120, top 120, bottom 80 -> max Y = 2400 - 120 - 120 = 2160
        assertThat(BubbleGeometry.clampVerticalOffset(50, screenHeightPx = 2400, bubbleHeightPx = 120))
            .isEqualTo(80)
        assertThat(BubbleGeometry.clampVerticalOffset(3000, screenHeightPx = 2400, bubbleHeightPx = 120))
            .isEqualTo(2160)
        assertThat(BubbleGeometry.clampVerticalOffset(500, screenHeightPx = 2400, bubbleHeightPx = 120))
            .isEqualTo(500)
    }

    @Test
    fun rms_silence_near_min_scale() {
        assertThat(BubbleGeometry.rmsScaleY(0f)).isWithin(0.001f).of(0.95f)
    }

    @Test
    fun rms_loud_near_max_scale() {
        assertThat(BubbleGeometry.rmsScaleY(10f)).isWithin(0.001f).of(1.05f)
    }

    @Test
    fun rms_clamps_out_of_range() {
        assertThat(BubbleGeometry.rmsScaleY(-5f)).isWithin(0.001f).of(0.95f)
        assertThat(BubbleGeometry.rmsScaleY(99f)).isWithin(0.001f).of(1.05f)
    }

    @Test
    fun cornerRadius_matches_shapes() {
        assertThat(BubbleGeometry.cornerRadiusDp("pill", 2f)).isEqualTo(24f)
        assertThat(BubbleGeometry.cornerRadiusDp("square", 2f)).isEqualTo(4f)
        assertThat(BubbleGeometry.cornerRadiusDp("circle", 2f)).isEqualTo(1998f)
    }

    @Test
    fun parkYAboveIme_lifts_below_keyboard() {
        assertThat(BubbleGeometry.parkYAboveIme(325, imeHeightPx = 800, gapPx = 24))
            .isEqualTo(824)
    }

    @Test
    fun parkYAboveIme_keeps_already_above() {
        assertThat(BubbleGeometry.parkYAboveIme(900, imeHeightPx = 800, gapPx = 24))
            .isEqualTo(900)
    }

    @Test
    fun parkYAboveIme_ime_zero_keeps_y() {
        assertThat(BubbleGeometry.parkYAboveIme(325, imeHeightPx = 0, gapPx = 24))
            .isEqualTo(325)
    }

    @Test
    fun imeHeightFromBounds_uses_rect() {
        assertThat(
            BubbleGeometry.imeHeightFromBounds(top = 1592, bottom = 2392, screenHeightPx = 2392)
        ).isEqualTo(800)
    }

    @Test
    fun imeHeightFromBounds_bad_rect_is_zero() {
        assertThat(
            BubbleGeometry.imeHeightFromBounds(top = 2000, bottom = 1000, screenHeightPx = 2392)
        ).isEqualTo(0)
    }

    @Test
    fun overlaySizePx_idle_is_52dp_square() {
        val (w, h) = BubbleGeometry.overlaySizePx(listening = false, density = 2.625f)
        // 52dp * 2.625 = 136.5 → 136px
        assertThat(w).isEqualTo(136)
        assertThat(h).isEqualTo(136)
    }

    @Test
    fun overlaySizePx_listen_is_small_bar_not_screen() {
        val (w, h) = BubbleGeometry.overlaySizePx(listening = true, density = 2.625f)
        assertThat(w).isAtMost((280 * 2.625f).toInt())
        assertThat(h).isAtMost((64 * 2.625f).toInt())
        assertThat(w).isGreaterThan(0)
        assertThat(h).isGreaterThan(0)
    }
}
