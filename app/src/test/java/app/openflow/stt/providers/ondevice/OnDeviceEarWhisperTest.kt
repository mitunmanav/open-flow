package app.openflow.stt.providers.ondevice

import app.openflow.stt.SpeechEngine
import app.openflow.whisper.PcmSource
import app.openflow.whisper.WhisperRuntime
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class OnDeviceEarWhisperTest {

    @Test
    fun stop_flush_emits_final_from_runtime() {
        val runtime = FakeRuntime()
        val ear = OnDeviceEar(
            modelFile = File("ok"),
            micGranted = true,
            runtime = runtime,
            pcm = object : PcmSource {
                override fun take() = FloatArray(1600) { 0.01f }
            },
        )
        val rec = RecordingListener()
        ear.setListener(rec)
        ear.startContinuous("en-IN")
        var done = false
        ear.stopAndFlush(1_000) { done = true }
        assertThat(rec.finals).contains("hello from whisper")
        assertThat(done).isTrue()
        assertThat(rec.listening).containsExactly(true, false).inOrder()
    }

    @Test
    fun start_without_model_is_fatal() {
        val ear = OnDeviceEar(modelFile = null, micGranted = true, runtime = null)
        val rec = RecordingListener()
        ear.setListener(rec)
        ear.startContinuous("en-US")
        assertThat(rec.errors).contains("model not installed")
        assertThat(rec.fatal).isTrue()
    }

    private class FakeRuntime : WhisperRuntime {
        override fun isLoaded() = true
        override fun transcribe(samples: FloatArray): String = "hello from whisper"
        override fun release() {}
    }

    private class RecordingListener : SpeechEngine.Listener {
        val finals = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val listening = mutableListOf<Boolean>()
        var fatal: Boolean = false

        override fun onPartial(text: String) {}
        override fun onFinal(text: String) { finals += text }
        override fun onError(message: String, fatal: Boolean) {
            errors += message
            this.fatal = fatal
        }
        override fun onReady() {}
        override fun onListeningChanged(listening: Boolean) {
            this.listening += listening
        }
    }
}
