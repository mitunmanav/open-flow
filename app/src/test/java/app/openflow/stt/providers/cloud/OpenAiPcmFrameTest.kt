package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Base64

class OpenAiPcmFrameTest {

    @Test
    fun start_sends_base64_append_not_raw_binary() {
        val sock = FakeSocket()
        val pcm = FakePcm()
        val ear = OpenAiRealtimeEar(apiKey = { "sk" }, socket = sock, pcm = pcm)
        ear.startOnce("en-US")
        assertThat(sock.headers["OpenAI-Beta"]).isEqualTo("realtime=v1")
        assertThat(sock.sentText[0]).contains("session.update")
        val chunk = byteArrayOf(1, 2, 3, 4)
        pcm.emit(chunk)
        assertThat(sock.sentBytes).isEmpty()
        assertThat(sock.sentText).hasSize(2)
        val msg = sock.sentText[1]
        assertThat(msg).contains("\"type\":\"input_audio_buffer.append\"")
        val up = PcmResample.upsample16kTo24k(chunk)
        assertThat(msg).contains(Base64.getEncoder().encodeToString(up))
        assertThat(msg).doesNotContain(Base64.getEncoder().encodeToString(chunk))
        ear.stop()
        assertThat(sock.sentText).contains("""{"type":"input_audio_buffer.commit"}""")
    }

    private class FakePcm : PcmSource {
        private var sink: ((ByteArray) -> Unit)? = null
        override fun start(onChunk: (ByteArray) -> Unit) {
            sink = onChunk
        }
        override fun stop() {
            sink = null
        }
        fun emit(bytes: ByteArray) {
            sink?.invoke(bytes)
        }
    }
}
