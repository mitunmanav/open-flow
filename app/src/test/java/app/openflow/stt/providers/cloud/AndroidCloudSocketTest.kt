package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Test
import java.io.IOException

class AndroidCloudSocketTest {

    @Test
    fun blocked_url_throws() {
        val socket = AndroidCloudSocket(allowUrl = { false })
        try {
            socket.connect("wss://evil.example/stt", emptyMap()) { }
            throw AssertionError("expected IOException")
        } catch (e: IOException) {
            assertThat(e.message).contains("blocked")
        }
    }

    @Test
    fun allowed_url_opens_and_sends() {
        val fake = RecordingWs()
        val socket = AndroidCloudSocket(
            open = { req, _ ->
                fake.lastUrl = req.url.toString()
                fake.lastAuth = req.header("Authorization")
                fake
            },
            allowUrl = { true },
        )
        val session = socket.connect(
            "wss://api.deepgram.com/v1/listen",
            mapOf("Authorization" to "Token secret"),
        ) { }
        assertThat(fake.lastUrl).contains("api.deepgram.com/v1/listen")
        assertThat(fake.lastAuth).isEqualTo("Token secret")
        session.send(byteArrayOf(1, 2))
        session.sendText("{\"type\":\"KeepAlive\"}")
        session.close()
        assertThat(fake.sentBinary).isEqualTo(1)
        assertThat(fake.sentText).isEqualTo(1)
        assertThat(fake.closed).isTrue()
    }

    @Test
    fun onFailure_invokes_onError() {
        var captured: WebSocketListener? = null
        val fake = RecordingWs()
        val socket = AndroidCloudSocket(
            open = { _, listener ->
                captured = listener
                fake
            },
            allowUrl = { true },
        )
        val errors = mutableListOf<String>()
        socket.connect(
            url = "wss://api.deepgram.com/v1/listen",
            headers = emptyMap(),
            onError = { errors += it },
            onText = { },
        )
        captured!!.onFailure(fake, IOException("boom"), null as Response?)
        assertThat(errors).containsExactly("boom")
    }

    @Test
    fun socket_fail_surfaces_on_cloud_ear() {
        val sock = FakeSocket()
        val ear = DeepgramEar(apiKey = { "k" }, socket = sock)
        val lis = RecListener()
        ear.setListener(lis)
        ear.startOnce("en-US")
        assertThat(lis.ready).isEqualTo(1)
        sock.fail("cloud socket failed")
        assertThat(lis.errors).containsExactly("cloud socket failed")
        assertThat(lis.fatal).containsExactly(true)
        assertThat(lis.listening.last()).isFalse()
    }

    private class RecordingWs : WebSocket {
        var lastUrl: String? = null
        var lastAuth: String? = null
        var sentBinary = 0
        var sentText = 0
        var closed = false
        private val req = Request.Builder().url("wss://api.deepgram.com/v1/listen").build()

        override fun request(): Request = req
        override fun queueSize(): Long = 0L
        override fun send(text: String): Boolean {
            sentText++
            return true
        }

        override fun send(bytes: ByteString): Boolean {
            sentBinary++
            return true
        }

        override fun close(code: Int, reason: String?): Boolean {
            closed = true
            return true
        }

        override fun cancel() {
            closed = true
        }
    }
}
