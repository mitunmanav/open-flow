package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.text.Charsets

class WavPcmTest {

    @Test
    fun wrap_adds_riff_header_and_keeps_pcm() {
        val pcm = byteArrayOf(1, 0, 2, 0)
        val wav = WavPcm.wrapPcm16leMono(pcm, sampleRate = 16_000)
        assertThat(wav.size).isEqualTo(44 + pcm.size)
        assertThat(wav.copyOfRange(0, 4).toString(Charsets.US_ASCII)).isEqualTo("RIFF")
        assertThat(wav.copyOfRange(8, 12).toString(Charsets.US_ASCII)).isEqualTo("WAVE")
        assertThat(wav.copyOfRange(44, wav.size)).isEqualTo(pcm)
        val rate = ByteBuffer.wrap(wav, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertThat(rate).isEqualTo(16_000)
    }
}
