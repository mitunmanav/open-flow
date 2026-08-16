package app.openflow.bubble

import app.openflow.ai.TextAIProvider
import app.openflow.text.CleanupLevel
import app.openflow.text.Feature
import app.openflow.text.FeatureAuto
import app.openflow.text.TextPostProcessor
import app.openflow.text.WritingStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PolishSessionPipelineTest {

    private class FakeBrain(var returnText: String = "Formatted output text.") : TextAIProvider {
        override val name: String = "fake"
        var lastInput: String? = null
        var lastTask: String? = null
        var callCount = 0

        override suspend fun enhance(text: String, task: String): String {
            callCount++
            lastInput = text
            lastTask = task
            return returnText
        }
    }

    @Test
    fun polish_pipeline_invokes_brain_enhance_when_brain_id_passed() {
        val brain = FakeBrain(returnText = "Hello world, this is polished.")
        val result = TextPostProcessor.polishSessionResult(
            raw = "um hello world this is polished",
            style = WritingStyle.CASUAL,
            level = CleanupLevel.HIGH,
            brain = brain,
            brainRewrite = true,
            earId = "system",
            brainId = "sarvam",
        )

        assertThat(brain.callCount).isAtLeast(1)
        assertThat(brain.lastInput).isNotEmpty()
        assertThat(result.clean).isEqualTo("Hello world, this is polished.")
    }

    @Test
    fun polish_pipeline_processes_dictionary_and_snippets_with_brain() {
        val brain = FakeBrain(returnText = "Meet me at Acme Corp tomorrow.")
        val dict = mapOf("acme" to "Acme Corp")
        val snippets = mapOf("tmrw" to "tomorrow")

        val result = TextPostProcessor.polishSessionResult(
            raw = "meet me at acme tmrw",
            style = WritingStyle.CASUAL,
            level = CleanupLevel.HIGH,
            dictionary = dict,
            snippets = snippets,
            brain = brain,
            brainRewrite = true,
            earId = "system",
            brainId = "openai",
        )

        assertThat(brain.callCount).isAtLeast(1)
        assertThat(result.clean).isEqualTo("Meet me at Acme Corp tomorrow.")
    }

    @Test
    fun feature_auto_detects_cloud_brain_capabilities() {
        val features = FeatureAuto.of(earId = "system", brainId = "sarvam")
        assertThat(features).contains(Feature.HIGH_AI)
        assertThat(features).contains(Feature.COMMAND)
    }
}
