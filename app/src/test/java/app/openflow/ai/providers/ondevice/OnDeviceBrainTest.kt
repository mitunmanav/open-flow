package app.openflow.ai.providers.ondevice

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OnDeviceBrainTest {

    @Test
    fun name_is_on_phone() {
        assertThat(OnDeviceBrain().name).isEqualTo("on_phone")
    }

    @Test
    fun no_file_returns_text_unchanged() = runTest {
        val brain = OnDeviceBrain(modelFile = null)
        assertThat(brain.enhance("hello world")).isEqualTo("hello world")
        assertThat(brain.enhance("", mode = "cleanup")).isEqualTo("")
        assertThat(brain.enhance("  spaced  ", mode = "formal")).isEqualTo("  spaced  ")
    }

    @Test
    fun no_file_cannot_rewrite() {
        assertThat(OnDeviceBrain(modelFile = null).rewrite).isFalse()
    }
}
