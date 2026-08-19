package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpeechEngineFlushTest {

    @Test
    fun default_stopAndFlush_calls_stop_then_done() {
        val ear = object : SpeechEngine {
            var stopped = false
            override val isAvailable = true
            override fun hasMicPermission() = true
            override fun setListener(listener: SpeechEngine.Listener?) {}
            override fun startContinuous(languageTag: String) {}
            override fun startOnce(languageTag: String) {}
            override fun stop() { stopped = true }
            override fun destroy() {}
        }
        var done = false
        ear.stopAndFlush(50) { done = true }
        assertThat(ear.stopped).isTrue()
        assertThat(done).isTrue()
    }
}
