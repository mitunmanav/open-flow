package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FailSoftSocketTest {

    @Test
    fun connect_never_throws_returns_dead_session() {
        val socket = FailSoftSocket()
        val session = socket.connect(
            "wss://example.invalid/stt",
            mapOf("Authorization" to "Bearer sk-secret"),
        ) { }
        assertThat(session).isNotNull()
        session.send(byteArrayOf(1, 2, 3))
        session.sendText("hi")
        session.close()
        session.close()
    }

    @Test
    fun connect_bad_url_still_returns_session() {
        val socket = FailSoftSocket()
        val a = socket.connect("", emptyMap()) { }
        val b = socket.connect("not-a-url", emptyMap()) { }
        a.send(ByteArray(0))
        b.sendText("")
        a.close()
        b.close()
        assertThat(a).isNotNull()
        assertThat(b).isNotNull()
    }
}
