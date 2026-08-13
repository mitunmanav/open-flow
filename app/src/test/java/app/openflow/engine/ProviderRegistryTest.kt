package app.openflow.engine

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import app.openflow.stt.SpeechEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderRegistryTest {

    @Test
    fun empty_registry_returns_system_ear_and_no_ai() {
        val system = FakeEar("system")
        val registry = ProviderRegistry(
            fallbackEar = { system },
            fallbackBrain = { NoAI },
        )
        assertThat(registry.ear(EarId.OPENAI)).isSameInstanceAs(system)
        assertThat(registry.ear("missing")).isSameInstanceAs(system)
        assertThat(registry.brain(BrainId.GROK)).isSameInstanceAs(NoAI)
        assertThat(registry.brain("nope")).isSameInstanceAs(NoAI)
    }

    @Test
    fun registered_fake_is_returned() {
        val system = FakeEar("system")
        val cloud = FakeEar("deepgram")
        val fakeBrain = FakeBrain("openai")
        val registry = ProviderRegistry(
            fallbackEar = { system },
            fallbackBrain = { NoAI },
        )
        registry.registerEar(EarId.DEEPGRAM) { cloud }
        registry.registerBrain(BrainId.OPENAI) { fakeBrain }
        assertThat(registry.ear(EarId.DEEPGRAM)).isSameInstanceAs(cloud)
        assertThat(registry.ear("deepgram")).isSameInstanceAs(cloud)
        assertThat(registry.brain(BrainId.OPENAI)).isSameInstanceAs(fakeBrain)
        assertThat(registry.brain("openai")).isSameInstanceAs(fakeBrain)
        assertThat(registry.ear(EarId.SYSTEM)).isSameInstanceAs(system)
        assertThat(registry.brain(BrainId.NONE)).isSameInstanceAs(NoAI)
    }

    @Test
    fun throwing_factory_falls_back_and_does_not_crash() {
        val system = FakeEar("system")
        val registry = ProviderRegistry(
            fallbackEar = { system },
            fallbackBrain = { NoAI },
        )
        registry.registerEar(EarId.LAPTOP) { error("boom") }
        registry.registerBrain(BrainId.CUSTOM) { error("boom") }
        assertThat(registry.ear(EarId.LAPTOP)).isSameInstanceAs(system)
        assertThat(registry.brain(BrainId.CUSTOM)).isSameInstanceAs(NoAI)
    }
}

private class FakeEar(private val tag: String) : SpeechEngine {
    override val isAvailable: Boolean = true
    override fun hasMicPermission(): Boolean = true
    override fun setListener(listener: SpeechEngine.Listener?) {}
    override fun startContinuous(languageTag: String) {}
    override fun startOnce(languageTag: String) {}
    override fun stop() {}
    override fun destroy() {}
    override fun toString(): String = "FakeEar($tag)"
}

private class FakeBrain(override val name: String) : TextAIProvider {
    override suspend fun enhance(text: String, mode: String): String = text
}
