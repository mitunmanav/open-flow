package app.openflow.text

import app.openflow.ai.TextAIProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CommandModeTest {

    private class FakeBrain : TextAIProvider {
        override val name: String = "fake"
        val modes = mutableListOf<String>()

        override suspend fun enhance(text: String, mode: String): String {
            modes += mode
            return text.uppercase()
        }
    }

    @Test
    fun off_returns_original() = runTest {
        val fake = FakeBrain()
        val out = CommandMode.apply("make bullets", brainCommand = false, brain = fake)
        assertThat(out).isEqualTo("make bullets")
        assertThat(fake.modes).isEmpty()
    }

    @Test
    fun on_calls_enhance_command() = runTest {
        val fake = FakeBrain()
        val out = CommandMode.apply("make this shorter", brainCommand = true, brain = fake)
        assertThat(out).isEqualTo("MAKE THIS SHORTER")
        assertThat(fake.modes).containsExactly("command")
    }
}
