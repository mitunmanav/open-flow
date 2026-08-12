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
}
