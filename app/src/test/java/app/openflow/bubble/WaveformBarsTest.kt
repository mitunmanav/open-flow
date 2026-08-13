package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WaveformBarsTest {

    @Test
    fun four_cells() {
        assertThat(WaveformBars.fromRms(0f)).hasLength(4)
        assertThat(WaveformBars.fromRms(10f)).hasLength(4)
    }

    @Test
    fun louder_fills_more() {
        val quiet = WaveformBars.fromRms(0f).count { it == '▮' }
        val loud = WaveformBars.fromRms(10f).count { it == '▮' }
        assertThat(loud).isGreaterThan(quiet)
        assertThat(loud).isEqualTo(4)
    }

    @Test
    fun clamps() {
        assertThat(WaveformBars.fromRms(-5f)).isEqualTo(WaveformBars.fromRms(0f))
        assertThat(WaveformBars.fromRms(99f)).isEqualTo(WaveformBars.fromRms(10f))
    }
}
