package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Base64

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
    fun connects_official_ws_with_mode_and_flush() {
        val sock = FakeSocket()
        val pcm = FakePcm()
        val rec = RecListener()
        val ear = SarvamEar(apiKey = { "srv-key" }, socket = sock, mode = "translate", pcm = pcm)
        ear.setListener(rec)
        ear.startContinuous("hi-IN")
        assertThat(sock.url).startsWith("wss://api.sarvam.ai/speech-to-text/ws")
        assertThat(sock.url).contains("model=saaras:v3")
        assertThat(sock.url).contains("mode=translate")
        assertThat(sock.url).contains("language-code=hi-IN")
        assertThat(sock.url).contains("sample_rate=16000")
        assertThat(sock.url).contains("input_audio_codec=wav")
        assertThat(sock.headers["api-subscription-key"]).isEqualTo("srv-key")
        val chunk = byteArrayOf(9, 8, 7, 6)
        pcm.emit(chunk)
        assertThat(sock.sentBytes).isEmpty()
        val payload = sock.sentText[0]
        val b64 = Regex("\"data\":\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
        assertThat(b64).isNotNull()
        val decoded = Base64.getDecoder().decode(b64)
        assertThat(decoded.copyOfRange(0, 4)).isEqualTo("RIFF".toByteArray())
        assertThat(decoded.copyOfRange(44, decoded.size)).isEqualTo(chunk)
        assertThat(payload).contains("\"encoding\":\"audio/wav\"")
        assertThat(sock.sentText[0]).contains("\"sample_rate\":\"16000\"")
        sock.push("""{"type":"data","data":{"transcript":"namaste duniya","request_id":"r1","metrics":{"audio_duration":1.0,"processing_latency":0.1}}}""")
        assertThat(rec.finals).contains("namaste duniya")
        ear.stop()
        assertThat(sock.sentText).contains("""{"type":"flush"}""")
    }

    @Test
    fun legacy_event_text_still_parses() {
        val sock = FakeSocket()
        val rec = RecListener()
        val ear = SarvamEar(apiKey = { "k" }, socket = sock)
        ear.setListener(rec)
        ear.startOnce("en-IN")
        sock.push("""{"event":"transcript.partial","text":"hi"}""")
        sock.push("""{"event":"transcript.final","text":"hi there"}""")
        assertThat(rec.partials).contains("hi")
        assertThat(rec.finals).contains("hi there")
    }

    @Test
    fun default_mode_is_transcribe() {
        val sock = FakeSocket()
        SarvamEar(apiKey = { "k" }, socket = sock).apply {
            setListener(RecListener())
            startOnce("en-IN")
        }
        assertThat(sock.url).contains("mode=transcribe")
        assertThat(sock.url).contains("language-code=en-IN")
    }

    @Test
    fun en_us_maps_to_en_in() {
        assertThat(SarvamEar.sarvamLanguage("en-US")).isEqualTo("en-IN")
        val sock = FakeSocket()
        SarvamEar(apiKey = { "k" }, socket = sock).apply {
            setListener(RecListener())
            startOnce("en-US")
        }
        assertThat(sock.url).contains("language-code=en-IN")
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
