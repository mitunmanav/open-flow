package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SarvamEarTest {

    @Test
    fun missing_key_on_error_no_crash() {
        val rec = RecListener()
        val ear = SarvamEar(apiKey = { "" }, socket = FakeSocket())
        ear.setListener(rec)
        ear.startContinuous("hi-IN")
        assertThat(rec.errors.joinToString()).contains("key")
        ear.destroy()
    }

    @Test
    fun connects_saaras_v3_realtime_with_mode() {
        val sock = FakeSocket()
        val rec = RecListener()
        val ear = SarvamEar(apiKey = { "srv-key" }, socket = sock, mode = "translate")
        ear.setListener(rec)
        ear.startContinuous("hi-IN")
        assertThat(sock.url).startsWith("wss://api.sarvam.ai/speech-to-text-realtime/ws")
        assertThat(sock.url).contains("model=saaras:v3-realtime")
        assertThat(sock.url).contains("mode=translate")
        assertThat(sock.url).contains("language_code=hi-IN")
        assertThat(sock.headers["api-subscription-key"]).isEqualTo("srv-key")
        sock.push("""{"event":"transcript.partial","text":"namaste"}""")
        sock.push("""{"event":"transcript.final","text":"namaste duniya"}""")
        assertThat(rec.partials).contains("namaste")
        assertThat(rec.finals).contains("namaste duniya")
        ear.stop()
    }

    @Test
    fun default_mode_is_transcribe() {
        val sock = FakeSocket()
        SarvamEar(apiKey = { "k" }, socket = sock).apply {
            setListener(RecListener())
            startOnce("en-IN")
        }
        assertThat(sock.url).contains("mode=transcribe")
        assertThat(sock.url).contains("language_code=en-IN")
    }
}
