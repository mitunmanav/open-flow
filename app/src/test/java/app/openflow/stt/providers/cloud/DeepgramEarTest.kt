package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeepgramEarTest {

    @Test
    fun missing_key_on_error_no_crash() {
        val sock = FakeSocket()
        val rec = RecListener()
        val ear = DeepgramEar(apiKey = { "  " }, socket = sock)
        ear.setListener(rec)
        ear.startOnce("en-US")
        assertThat(rec.errors.joinToString()).contains("key")
        assertThat(sock.url).isNull()
        ear.destroy()
    }

    @Test
    fun connects_listen_with_token() {
        val sock = FakeSocket()
        val rec = RecListener()
        val ear = DeepgramEar(apiKey = { "dg-key" }, socket = sock)
        ear.setListener(rec)
        ear.startContinuous("en-IN")
        assertThat(sock.url).isEqualTo(
            "wss://api.deepgram.com/v1/listen" +
                "?model=nova-2&encoding=linear16&sample_rate=16000&channels=1" +
                "&interim_results=true&punctuate=true&smart_format=true" +
                "&language=en-IN",
        )
        assertThat(sock.headers["Authorization"]).isEqualTo("Token dg-key")
        sock.push("""{"is_final":false,"channel":{"alternatives":[{"transcript":"hello"}]}}""")
        sock.push("""{"is_final":true,"channel":{"alternatives":[{"transcript":"hello world"}]}}""")
        assertThat(rec.partials).contains("hello")
        assertThat(rec.finals).contains("hello world")
        ear.stop()
        assertThat(sock.sentText).contains("""{"type":"CloseStream"}""")
        assertThat(sock.closed).isTrue()
    }
}
