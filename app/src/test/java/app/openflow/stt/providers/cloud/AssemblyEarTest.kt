package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssemblyEarTest {

    @Test
    fun missing_key_on_error_no_crash() {
        val rec = RecListener()
        val ear = AssemblyEar(apiKey = { "" }, socket = FakeSocket())
        ear.setListener(rec)
        ear.startContinuous("en-US")
        assertThat(rec.errors.joinToString()).contains("key")
        ear.destroy()
    }

    @Test
    fun connects_v3_ws_raw_authorization() {
        val sock = FakeSocket()
        val rec = RecListener()
        val ear = AssemblyEar(apiKey = { "asm-key" }, socket = sock)
        ear.setListener(rec)
        ear.startContinuous("en-US")
        assertThat(sock.url).isEqualTo("wss://streaming.assemblyai.com/v3/ws?sample_rate=16000")
        assertThat(sock.headers["Authorization"]).isEqualTo("asm-key")
        sock.push("""{"type":"Turn","transcript":"hey","end_of_turn":false}""")
        sock.push("""{"type":"Turn","transcript":"hey there","end_of_turn":true}""")
        assertThat(rec.partials).contains("hey")
        assertThat(rec.finals).contains("hey there")
        ear.stop()
        assertThat(sock.sentText).contains("""{"type":"Terminate"}""")
    }
}
