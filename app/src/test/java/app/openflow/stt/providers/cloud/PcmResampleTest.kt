package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmResampleTest {

    @Test
    fun empty_stays_empty() {
        assertThat(PcmResample.upsample16kTo24k(ByteArray(0))).isEmpty()
    }

    @Test
    fun two_samples_become_three() {
        val src = shortsToLe(shortArrayOf(0, 3000))
        val out = PcmResample.upsample16kTo24k(src)
        assertThat(out.size).isEqualTo(6)
        val samples = leToShorts(out)
        assertThat(samples).hasLength(3)
        assertThat(samples[0]).isEqualTo(0)
        assertThat(samples[2]).isEqualTo(3000)
        assertThat(samples[1]).isEqualTo(1500)
    }

    @Test
    fun length_is_three_halves() {
        val src = shortsToLe(ShortArray(160) { it.toShort() })
        val out = PcmResample.upsample16kTo24k(src)
        assertThat(out.size).isEqualTo(160 * 3) // 240 samples * 2 bytes
    }

    private fun shortsToLe(s: ShortArray): ByteArray {
        val buf = ByteBuffer.allocate(s.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        s.forEach { buf.putShort(it) }
        return buf.array()
    }

    private fun leToShorts(b: ByteArray): ShortArray {
        val buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        return ShortArray(b.size / 2) { buf.short }
    }
}
