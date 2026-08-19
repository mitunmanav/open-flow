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

    @Test
    fun start_fails_loud_when_pcm_refuses() {
        val sock = FakeSocket()
        val pcm = FakePcm(ok = false)
        val ear = DeepgramEar(apiKey = { "dg" }, socket = sock, pcm = pcm)
        val rec = RecListener()
        ear.setListener(rec)
        ear.startOnce("en-US")
        assertThat(pcm.started).isTrue()
        assertThat(rec.errors).contains("Microphone start failed")
        assertThat(rec.fatal).contains(true)
        assertThat(rec.ready).isEqualTo(0)
        assertThat(rec.listening).isEmpty()
        assertThat(sock.closed).isTrue()
    }

    private class FakePcm(private val ok: Boolean = true) : PcmSource {
        var started = false
        var stopped = false
        private var sink: ((ByteArray) -> Unit)? = null

        override fun start(onChunk: (ByteArray) -> Unit): Boolean {
            started = true
            stopped = false
            sink = onChunk
            return ok
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
