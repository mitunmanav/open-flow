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
        assertThat(BubbleGeometry.cornerRadiusDp("pill", 2f)).isEqualTo(48f)
        assertThat(BubbleGeometry.cornerRadiusDp("square", 2f)).isEqualTo(32f)
        assertThat(BubbleGeometry.cornerRadiusDp("circle", 2f)).isEqualTo(1998f)
    }
}
