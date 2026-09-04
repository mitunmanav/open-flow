package app.openflow.engine

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import app.openflow.stt.SpeechEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderRegistryTest {

    @Test
    fun empty_registry_returns_null_for_unregistered() {
        // The registry is honest about wiring: missing factories return null.
        // Callers that want a fallback (system ear / no-AI) must resolve the id
        // through EarGate or use AppEngineWire.currentEar/currentBrain.
        val system = FakeEar("system")
        val registry = ProviderRegistry(
            fallbackEar = { system },
            fallbackBrain = { NoAI },
        )
        assertThat(registry.ear(EarId.OPENAI)).isNull()
        assertThat(registry.ear("missing")).isNull()
        assertThat(registry.brain(BrainId.GROK)).isNull()
        assertThat(registry.brain("nope")).isNull()
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
        registry.registerEar(EarId.SYSTEM) { system }
        registry.registerEar(EarId.DEEPGRAM) { cloud }
        registry.registerBrain(BrainId.NONE) { NoAI }
        registry.registerBrain(BrainId.OPENAI) { fakeBrain }
        assertThat(registry.ear(EarId.DEEPGRAM)).isSameInstanceAs(cloud)
        assertThat(registry.ear("deepgram")).isSameInstanceAs(cloud)
        assertThat(registry.brain(BrainId.OPENAI)).isSameInstanceAs(fakeBrain)
        assertThat(registry.brain("openai")).isSameInstanceAs(fakeBrain)
        assertThat(registry.ear(EarId.SYSTEM)).isSameInstanceAs(system)
        assertThat(registry.brain(BrainId.NONE)).isSameInstanceAs(NoAI)
        // Unregistered ids stay null (the new contract).
        assertThat(registry.ear(EarId.OPENAI)).isNull()
        assertThat(registry.brain(BrainId.GROK)).isNull()
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
