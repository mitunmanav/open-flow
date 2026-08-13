package app.openflow.stt.providers.ondevice

import app.openflow.stt.SpeechEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnDeviceEarTest {

    @Test
    fun no_model_file_errors_on_start_continuous() {
        val ear = OnDeviceEar(modelFile = null)
        val listener = RecordingListener()
        ear.setListener(listener)
        ear.startContinuous("en-US")
        assertThat(listener.errors).contains("model not installed")
        assertThat(listener.fatal).isTrue()
    }

    @Test
    fun no_model_file_errors_on_start_once() {
        val ear = OnDeviceEar(modelFile = null)
        val listener = RecordingListener()
        ear.setListener(listener)
        ear.startOnce("en-US")
        assertThat(listener.errors).contains("model not installed")
        assertThat(listener.fatal).isTrue()
    }

    @Test
    fun missing_file_is_not_available() {
        assertThat(OnDeviceEar(modelFile = null).isAvailable).isFalse()
    }

    @Test
    fun start_without_listener_does_not_throw() {
        OnDeviceEar(modelFile = null).startOnce("en-US")
    }

    private class RecordingListener : SpeechEngine.Listener {
        val errors = mutableListOf<String>()
        var fatal: Boolean = false

        override fun onPartial(text: String) {}
        override fun onFinal(text: String) {}
        override fun onError(message: String, fatal: Boolean) {
            errors += message
            this.fatal = fatal
        }
        override fun onReady() {}
        override fun onListeningChanged(listening: Boolean) {}
    }
}
