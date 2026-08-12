package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShakeDetectorTest {
    @Test
    fun not_shake_at_rest() {
        val d = ShakeDetector()
        assertThat(d.onAccel(0f, 0f, 9.8f, nowMs = 0L)).isFalse()
    }

    @Test
    fun shake_when_g_force_high() {
        val d = ShakeDetector(thresholdG = 2.7f, minIntervalMs = 500L)
        // 30 m/s² ≈ 3.06g > 2.7g threshold
        assertThat(d.onAccel(30f, 0f, 0f, nowMs = 1000L)).isTrue()
    }

    @Test
    fun debounce_second_shake() {
        val d = ShakeDetector(thresholdG = 2.7f, minIntervalMs = 500L)
        assertThat(d.onAccel(30f, 0f, 0f, nowMs = 1000L)).isTrue()
        assertThat(d.onAccel(30f, 0f, 0f, nowMs = 1100L)).isFalse()
        assertThat(d.onAccel(30f, 0f, 0f, nowMs = 1600L)).isTrue()
    }
}
