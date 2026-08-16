package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class BubbleMotionTest {

    @Test
    fun slop_ignores_tiny_finger_jitter() {
        assertThat(BubbleMotion.passedSlop(dx = 4, dy = 3, slopPx = 16)).isFalse()
        assertThat(BubbleMotion.passedSlop(dx = 17, dy = 0, slopPx = 16)).isTrue()
        assertThat(BubbleMotion.passedSlop(dx = 0, dy = -17, slopPx = 16)).isTrue()
    }

    @Test
    fun shouldUpdateLayout_only_after_drag() {
        assertThat(BubbleMotion.shouldUpdateLayout(dragged = false)).isFalse()
        assertThat(BubbleMotion.shouldUpdateLayout(dragged = true)).isTrue()
    }

    @Test
    fun snapX_fling_right_goes_to_end_edge() {
        // Gravity.END: x≈0 is right. Finger-right velocity is positive.
        val right = BubbleMotion.snapX(
            x = 400,
            vxPxPerSec = 1200f,
            screenWidthPx = 1080,
            bubbleWidthPx = 120,
        )
        assertThat(right).isEqualTo(32)
    }

    @Test
    fun snapX_fling_left_goes_to_start_edge() {
        val left = BubbleMotion.snapX(
            x = 40,
            vxPxPerSec = -1200f,
            screenWidthPx = 1080,
            bubbleWidthPx = 120,
        )
        assertThat(left).isEqualTo(1080 - 120 - 32)
    }

    @Test
    fun snapX_slow_uses_nearest_edge() {
        val nearRight = BubbleMotion.snapX(
            x = 40,
            vxPxPerSec = 10f,
            screenWidthPx = 1080,
            bubbleWidthPx = 120,
        )
        assertThat(nearRight).isEqualTo(32)
    }

    @Test
    fun snapDuration_clamps() {
        assertThat(BubbleMotion.snapDurationMs(distancePx = 8, vxPxPerSec = 4000f)).isAtLeast(120L)
        assertThat(BubbleMotion.snapDurationMs(distancePx = 8, vxPxPerSec = 4000f)).isAtMost(280L)
        assertThat(BubbleMotion.snapDurationMs(distancePx = 900, vxPxPerSec = 10f)).isAtMost(280L)
        assertThat(abs(BubbleMotion.snapDurationMs(400, 800f) - 250L)).isAtMost(130L)
    }

    @Test
    fun skipImmediateXWrite_when_snapping() {
        assertThat(BubbleMotion.skipImmediateXWrite(snapping = true)).isTrue()
        assertThat(BubbleMotion.skipImmediateXWrite(snapping = false)).isFalse()
    }
}
