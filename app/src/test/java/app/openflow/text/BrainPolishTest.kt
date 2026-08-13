package app.openflow.text

import app.openflow.ai.TextAIProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BrainPolishTest {

    private class FakeBrain : TextAIProvider {
        override val name: String = "fake"
        var calls: Int = 0

        override suspend fun enhance(text: String, mode: String): String {
            calls += 1
            return text.uppercase()
        }
    }

    private val sample = "hello there friend this is a longer line"

    @Test
    fun high_with_rewrite_uppercases_after_rules() = runTest {
        val fake = FakeBrain()
        val rules = TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.HIGH,
        )
        val out = TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.HIGH,
            brain = fake,
            brainRewrite = true,
        )
        assertThat(out.clean).isEqualTo(rules.clean.uppercase())
        assertThat(out.raw).isEqualTo(sample)
        assertThat(fake.calls).isEqualTo(1)
    }

    @Test
    fun light_does_not_call_brain() = runTest {
        val fake = FakeBrain()
        TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.LIGHT,
            brain = fake,
            brainRewrite = true,
        )
        assertThat(fake.calls).isEqualTo(0)
    }

    @Test
    fun high_without_rewrite_does_not_call_brain() = runTest {
        val fake = FakeBrain()
        val out = TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.HIGH,
            brain = fake,
            brainRewrite = false,
        )
        assertThat(out.clean).isNotEqualTo(out.clean.uppercase())
        assertThat(fake.calls).isEqualTo(0)
    }
}
