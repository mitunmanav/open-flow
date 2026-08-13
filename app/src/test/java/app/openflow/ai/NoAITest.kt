package app.openflow.ai

import app.openflow.engine.ModelCapability
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
        assertThat(NoAI.enhance("keep", mode = "command")).isEqualTo("keep")
    }

    @Test
    fun capability_is_none_brain() {
        val c = NoAI.capability
        assertThat(c).isEqualTo(ModelCapability.noneBrain())
        assertThat(c.rewrite).isFalse()
        assertThat(c.commandMode).isFalse()
        assertThat(c.needsNet).isFalse()
        assertThat(c.audioLeavesDevice).isFalse()
    }
}
