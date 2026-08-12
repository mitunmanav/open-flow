package app.openflow.ai

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NoAITest {

    @Test
    fun name_is_none() {
        assertThat(NoAI.name).isEqualTo("none")
    }

    @Test
    fun enhance_returns_text_unchanged() = runTest {
        assertThat(NoAI.enhance("hello world")).isEqualTo("hello world")
        assertThat(NoAI.enhance("", mode = "cleanup")).isEqualTo("")
        assertThat(NoAI.enhance("  spaced  ", mode = "formal")).isEqualTo("  spaced  ")
    }

    @Test
    fun local_llm_stub_throws() = runTest {
        val p = LocalLLM("gemma-test")
        assertThat(p.name).isEqualTo("local:gemma-test")
        val err = runCatching { p.enhance("x") }.exceptionOrNull()
        assertThat(err).isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun remote_llm_stub_throws() = runTest {
        val p = RemoteLLM("openai")
        assertThat(p.name).isEqualTo("remote:openai")
        val err = runCatching { p.enhance("x", mode = "cleanup") }.exceptionOrNull()
        assertThat(err).isInstanceOf(UnsupportedOperationException::class.java)
    }
}
