package app.openflow.stt.providers.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OpenAiRealtimeEarTest {

    @Test
    fun missing_key_on_error_no_crash() {
        val sock = FakeSocket()
        val rec = RecListener()
        val ear = OpenAiRealtimeEar(apiKey = { "" }, socket = sock)
        ear.setListener(rec)
        ear.startContinuous("en-US")
        assertThat(rec.errors.joinToString()).contains("key")
        assertThat(rec.fatal.first()).isTrue()
        assertThat(sock.url).isNull()
        ear.startOnce("en-US")
        ear.stop()
        ear.destroy()
    }

    @Test
    fun connects_realtime_with_bearer() {
        val sock = FakeSocket()
        val rec = RecListener()
        val ear = OpenAiRealtimeEar(apiKey = { "sk-live" }, socket = sock)
        ear.setListener(rec)
        ear.startContinuous("en-US")
        assertThat(sock.url).isEqualTo("wss://api.openai.com/v1/realtime?intent=transcription")
        assertThat(sock.headers["Authorization"]).isEqualTo("Bearer sk-live")
        assertThat(rec.ready).isEqualTo(1)
        assertThat(rec.listening).contains(true)
        sock.push("""{"type":"conversation.item.input_audio_transcription.delta","delta":"hi"}""")
        sock.push("""{"type":"conversation.item.input_audio_transcription.completed","transcript":"hi there"}""")
        assertThat(rec.partials).contains("hi")
        assertThat(rec.finals).contains("hi there")
        ear.stop()
        assertThat(sock.closed).isTrue()
        assertThat(rec.listening.last()).isFalse()
    }

    @Test
    fun is_speech_engine() {
        val ear: app.openflow.stt.SpeechEngine =
            OpenAiRealtimeEar(apiKey = { "k" }, socket = FakeSocket())
        assertThat(ear.isAvailable).isTrue()
        assertThat(ear.hasMicPermission()).isTrue()
    }
}
