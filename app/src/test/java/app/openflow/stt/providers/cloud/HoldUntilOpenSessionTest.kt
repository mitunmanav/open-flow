package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HoldUntilOpenSessionTest {

    @Test
    fun send_before_open_is_queued_then_flushed_in_order() {
        val inner = RecordingSession()
        val hold = HoldUntilOpenSession(inner)
        hold.sendText("first")
        hold.send(byteArrayOf(1, 2))
        hold.sendText("second")
        assertThat(inner.texts).isEmpty()
        assertThat(inner.binaries).isEmpty()
        hold.markOpen()
        assertThat(inner.texts).containsExactly("first", "second").inOrder()
        assertThat(inner.binaries).hasSize(1)
        assertThat(inner.binaries[0].toList()).containsExactly(1.toByte(), 2.toByte())
    }

    @Test
    fun send_after_open_goes_through() {
        val inner = RecordingSession()
        val hold = HoldUntilOpenSession(inner)
        hold.markOpen()
        hold.sendText("now")
        assertThat(inner.texts).containsExactly("now")
    }

    @Test
    fun close_drops_queue_and_closes_inner() {
        val inner = RecordingSession()
        val hold = HoldUntilOpenSession(inner)
        hold.sendText("lost")
        hold.close()
        assertThat(inner.closed).isTrue()
        assertThat(inner.texts).isEmpty()
        hold.markOpen()
        assertThat(inner.texts).isEmpty()
    }

    private class RecordingSession : CloudSession {
        val texts = mutableListOf<String>()
        val binaries = mutableListOf<ByteArray>()
        var closed = false
        override fun send(bytes: ByteArray) {
            binaries += bytes
        }
        override fun sendText(text: String) {
            texts += text
        }
        override fun close() {
            closed = true
        }
    }
}
