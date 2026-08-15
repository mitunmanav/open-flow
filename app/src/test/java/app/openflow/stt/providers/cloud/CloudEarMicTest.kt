package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CloudEarMicTest {

    @Test
    fun start_pumps_pcm_to_socket_stop_halts() {
        val sock = FakeSocket()
        val pcm = FakePcm()
        val ear = DeepgramEar(apiKey = { "dg" }, socket = sock, pcm = pcm)
        ear.startOnce("en-US")
        assertThat(pcm.started).isTrue()
        pcm.emit(byteArrayOf(1, 2, 3, 4))
        assertThat(sock.sentBytes).hasSize(1)
        assertThat(sock.sentBytes[0].toList()).isEqualTo(listOf(1.toByte(), 2, 3, 4))
        ear.stop()
        assertThat(pcm.stopped).isTrue()
        pcm.emit(byteArrayOf(9))
        assertThat(sock.sentBytes).hasSize(1)
    }

    private class FakePcm : PcmSource {
        var started = false
        var stopped = false
        private var sink: ((ByteArray) -> Unit)? = null

        override fun start(onChunk: (ByteArray) -> Unit) {
            started = true
            stopped = false
            sink = onChunk
        }

        override fun stop() {
            stopped = true
            sink = null
        }

        fun emit(bytes: ByteArray) {
            sink?.invoke(bytes)
        }
    }
}
