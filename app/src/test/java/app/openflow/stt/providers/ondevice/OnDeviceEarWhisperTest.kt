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

    @Test
    fun start_preloads_runtime() {
        val runtime = FakeRuntime()
        val ear = OnDeviceEar(
            modelFile = File("ok"),
            micGranted = true,
            runtime = runtime,
            pcm = object : PcmSource {
                override fun take() = FloatArray(0)
            },
        )
        ear.startContinuous("en-US")
        assertThat(runtime.preloads).isEqualTo(1)
    }

    @Test
    fun stop_flush_transcribes_each_chunk_then_joins() {
        val runtime = FakeRuntime()
        val ear = OnDeviceEar(
            modelFile = File("ok"),
            micGranted = true,
            runtime = runtime,
            pcm = object : PcmSource {
                override fun take() = FloatArray(10) { 0.2f }
            },
            chunkSamples = 4,
        )
        val rec = RecordingListener()
        ear.setListener(rec)
        ear.startContinuous("en-US")
        ear.stopAndFlush(1_000) {}
        assertThat(runtime.calls).isEqualTo(3)
        assertThat(rec.finals.single()).isEqualTo("hello from whisper hello from whisper hello from whisper")
    }

    @Test
    fun stop_flush_trims_quiet_edges() {
        val runtime = FakeRuntime()
        val pcm = FloatArray(20_000)
        for (i in 4_000 until 14_000) pcm[i] = 0.2f
        val ear = OnDeviceEar(
            modelFile = File("ok"),
            micGranted = true,
            runtime = runtime,
            pcm = object : PcmSource {
                override fun take() = pcm
            },
        )
        ear.setListener(RecordingListener())
        ear.startContinuous("en-US")
        ear.stopAndFlush(1_000) {}
        assertThat(runtime.lastSamples!!.size).isEqualTo(10_000)
    }

    private class FakeRuntime : WhisperRuntime {
        var preloads = 0
        var lastSamples: FloatArray? = null
        var calls = 0
        override fun isLoaded() = true
        override fun preload() { preloads++ }
        override fun transcribe(samples: FloatArray): String {
            calls++
            lastSamples = samples
            return "hello from whisper"
        }
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
