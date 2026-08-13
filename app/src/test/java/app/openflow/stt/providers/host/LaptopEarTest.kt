package app.openflow.stt.providers.host

import app.openflow.stt.SpeechEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LaptopEarTest {

    @Test
    fun missing_url_onError_not_crash() {
        val rec = Rec()
        val ear = LaptopEar(baseUrl = null)
        ear.setListener(rec)
        ear.startContinuous("en-US")
        assertThat(rec.errors).isNotEmpty()
        assertThat(rec.errors.first().second).isTrue()
        assertThat(ear.isAvailable).isFalse()
    }

    @Test
    fun bad_url_onError() {
        val rec = Rec()
        val ear = LaptopEar(baseUrl = "http://example.com/v1")
        ear.setListener(rec)
        ear.startOnce("en-US")
        assertThat(rec.errors).isNotEmpty()
        assertThat(LaptopEar(baseUrl = "file:///tmp").also {
            val r = Rec()
            it.setListener(r)
            it.startOnce("en-US")
            assertThat(r.errors).isNotEmpty()
        }.isAvailable).isFalse()
    }

    @Test
    fun lan_url_ready_no_fake_final() {
        val rec = Rec()
        val ear = LaptopEar(baseUrl = "http://192.168.0.10:9000/v1")
        ear.setListener(rec)
        ear.startOnce("en-US")
        assertThat(ear.isAvailable).isTrue()
        assertThat(rec.errors).isEmpty()
        assertThat(rec.ready).isTrue()
        assertThat(rec.listening).isTrue()
        assertThat(rec.finals).isEmpty()
        ear.stop()
        assertThat(rec.listening).isFalse()
    }

    @Test
    fun laptop_caps_match_plan() {
        val ear = LaptopEar(baseUrl = "http://127.0.0.1:11434/v1")
        assertThat(ear.streamLive).isTrue()
        assertThat(ear.audioLeavesDevice).isTrue()
        assertThat(ear.needsNet).isTrue()
    }

    private class Rec : SpeechEngine.Listener {
        val errors = mutableListOf<Pair<String, Boolean>>()
        val finals = mutableListOf<String>()
        var ready = false
        var listening = false

        override fun onPartial(text: String) {}
        override fun onFinal(text: String) {
            finals += text
        }
        override fun onError(message: String, fatal: Boolean) {
            errors += message to fatal
        }
        override fun onReady() {
            ready = true
        }
        override fun onListeningChanged(listening: Boolean) {
            this.listening = listening
        }
    }
}
