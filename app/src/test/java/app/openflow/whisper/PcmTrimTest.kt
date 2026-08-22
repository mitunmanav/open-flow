package app.openflow.whisper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PcmTrimTest {
    @Test
    fun drops_leading_and_trailing_quiet() {
        val samples = FloatArray(12_000)
        for (i in 4_000 until 8_000) samples[i] = 0.2f
        val out = PcmTrim.trim(samples, floor = 0.01f, minKeep = 100)
        assertThat(out.size).isEqualTo(4_000)
        assertThat(out[0]).isEqualTo(0.2f)
        assertThat(out.last()).isEqualTo(0.2f)
    }

    @Test
    fun all_quiet_keeps_original() {
        val samples = FloatArray(16_000)
        val out = PcmTrim.trim(samples, floor = 0.01f, minKeep = 8_000)
        assertThat(out).isSameInstanceAs(samples)
    }

    @Test
    fun too_short_after_trim_keeps_original() {
        val samples = FloatArray(16_000)
        samples[8_000] = 0.5f
        val out = PcmTrim.trim(samples, floor = 0.01f, minKeep = 8_000)
        assertThat(out).isSameInstanceAs(samples)
    }
}
