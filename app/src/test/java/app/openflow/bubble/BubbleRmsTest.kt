package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleRmsTest {

    @Test
    fun capture_clamps() {
        assertThat(BubbleRms.capture(-3f)).isEqualTo(0f)
        assertThat(BubbleRms.capture(4f)).isEqualTo(4f)
        assertThat(BubbleRms.capture(99f)).isEqualTo(10f)
    }

    @Test
    fun louder_rms_taller_bars() {
        val quiet = BubbleRms.bars(0f).count { it == '▮' }
        val loud = BubbleRms.bars(10f).count { it == '▮' }
        assertThat(loud).isGreaterThan(quiet)
        assertThat(BubbleRms.bars(10f)).isEqualTo(WaveformBars.fromRms(10f))
    }

    @Test
    fun louder_rms_bigger_pulse() {
        assertThat(BubbleRms.pulseScale(10f)).isGreaterThan(BubbleRms.pulseScale(0f))
        assertThat(BubbleRms.pulseScale(0f)).isEqualTo(1f)
    }
}
