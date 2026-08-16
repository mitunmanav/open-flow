package app.openflow.text

import app.openflow.ai.TextAIProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BrainPolishTest {

    @Before
    fun resetLearn() {
        LearnEngine.resetLearn()
    }

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
    fun light_with_rewrite_calls_brain() = runTest {
        val fake = FakeBrain()
        TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.LIGHT,
            brain = fake,
            brainRewrite = true,
        )
        assertThat(fake.calls).isEqualTo(1)
    }

    @Test
    fun medium_with_rewrite_calls_brain() = runTest {
        val fake = FakeBrain()
        TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.NORMAL,
            brain = fake,
            brainRewrite = true,
        )
        assertThat(fake.calls).isEqualTo(1)
    }

    @Test
    fun raw_never_calls_brain() = runTest {
        val fake = FakeBrain()
        TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.RAW,
            brain = fake,
            brainRewrite = true,
            brainId = "openai",
        )
        assertThat(fake.calls).isEqualTo(0)
    }

    @Test
    fun dict_reapplied_after_brain() = runTest {
        val fake = object : TextAIProvider {
            override val name: String = "fake"
            override suspend fun enhance(text: String, mode: String): String = "meet mike later"
        }
        val out = TextPostProcessor.polishSessionResult(
            raw = "meet mike later",
            level = CleanupLevel.LIGHT,
            dictionary = mapOf("mike" to "Mic"),
            brain = fake,
            brainRewrite = true,
        )
        assertThat(out.clean.lowercase()).isEqualTo("meet mic later")
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

    @Test
    fun high_plus_feature_auto_high_ai_calls_brain() = runTest {
        val fake = FakeBrain()
        TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.HIGH,
            brain = fake,
            brainId = "openai",
        )
        assertThat(fake.calls).isEqualTo(2)
    }

    @Test
    fun rewrite_brain_runs_command_mode() = runTest {
        val fake = TrackingBrain()
        TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.HIGH,
            brain = fake,
            brainId = "openai",
        )
        assertThat(fake.modes).containsExactly("cleanup", "command").inOrder()
    }

    private class TrackingBrain : TextAIProvider {
        override val name: String = "fake"
        val modes = mutableListOf<String>()

        override suspend fun enhance(text: String, mode: String): String {
            modes += mode
            return text
        }
    }

    @Test
    fun high_rules_brain_skips_enhance() = runTest {
        val fake = FakeBrain()
        TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.HIGH,
            brain = fake,
            brainId = "none",
        )
        assertThat(fake.calls).isEqualTo(0)
    }

    @Test
    fun high_on_phone_brain_skips_enhance() = runTest {
        val fake = FakeBrain()
        TextPostProcessor.polishSessionResult(
            raw = sample,
            level = CleanupLevel.HIGH,
            brain = fake,
            brainId = "on_phone",
        )
        assertThat(fake.calls).isEqualTo(0)
    }
}
