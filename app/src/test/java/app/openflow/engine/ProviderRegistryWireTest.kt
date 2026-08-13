package app.openflow.engine

import app.openflow.AppEngineWire
import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import app.openflow.ai.providers.cloud.AnthropicBrain
import app.openflow.ai.providers.cloud.CloudHttp
import app.openflow.ai.providers.cloud.OpenAiCompatBrain
import app.openflow.ai.providers.host.LaptopBrain
import app.openflow.ai.providers.ondevice.OnDeviceBrain
import app.openflow.prefs.MemoryPrefsStore
import app.openflow.secrets.MemorySecretStore
import app.openflow.stt.SpeechEngine
import app.openflow.stt.providers.cloud.AssemblyEar
import app.openflow.stt.providers.cloud.CloudSession
import app.openflow.stt.providers.cloud.CloudSocket
import app.openflow.stt.providers.cloud.DeepgramEar
import app.openflow.stt.providers.cloud.OpenAiRealtimeEar
import app.openflow.stt.providers.cloud.SarvamEar
import app.openflow.stt.providers.host.LaptopEar
import app.openflow.stt.providers.ondevice.OnDeviceEar
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderRegistryWireTest {

    private val ears = listOf(
        "system", "on_phone", "laptop", "openai", "deepgram", "assemblyai", "sarvam",
    )
    private val brains = listOf(
        "none", "on_phone", "laptop", "openai", "grok", "minimax", "deepseek",
        "gemini", "mistral", "together", "fireworks", "openrouter", "sarvam",
        "anthropic", "custom",
    )

    @Test
    fun wire_registers_all_ears_and_brains_without_keys() {
        val wired = wired()
        for (id in ears) {
            assertThat(wired.registry.ear(id)).isNotNull()
        }
        for (id in brains) {
            assertThat(wired.registry.brain(id)).isNotNull()
        }
        assertThat(wired.registry.ear("system")).isSameInstanceAs(wired.system)
        assertThat(wired.registry.ear("on_phone")).isInstanceOf(OnDeviceEar::class.java)
        assertThat(wired.registry.ear("laptop")).isInstanceOf(LaptopEar::class.java)
        assertThat(wired.registry.ear("openai")).isInstanceOf(OpenAiRealtimeEar::class.java)
        assertThat(wired.registry.ear("deepgram")).isInstanceOf(DeepgramEar::class.java)
        assertThat(wired.registry.ear("assemblyai")).isInstanceOf(AssemblyEar::class.java)
        assertThat(wired.registry.ear("sarvam")).isInstanceOf(SarvamEar::class.java)
        assertThat(wired.registry.brain("none")).isSameInstanceAs(NoAI)
        assertThat(wired.registry.brain("on_phone")).isInstanceOf(OnDeviceBrain::class.java)
        assertThat(wired.registry.brain("laptop")).isInstanceOf(LaptopBrain::class.java)
        assertThat(wired.registry.brain("openai")).isInstanceOf(OpenAiCompatBrain::class.java)
        assertThat(wired.registry.brain("grok")).isInstanceOf(OpenAiCompatBrain::class.java)
        assertThat(wired.registry.brain("grok").name).isEqualTo("grok")
        assertThat(wired.registry.brain("anthropic")).isInstanceOf(AnthropicBrain::class.java)
        assertThat(wired.registry.brain("custom")).isInstanceOf(OpenAiCompatBrain::class.java)
        assertThat(wired.registry.brain("groq")).isSameInstanceAs(NoAI)
    }

    @Test
    fun current_ear_and_brain_follow_prefs() {
        val wired = wired()
        wired.prefs.earId = "deepgram"
        wired.prefs.brainId = "grok"
        assertThat(AppEngineWire.currentEar(wired.registry, wired.prefs))
            .isInstanceOf(DeepgramEar::class.java)
        assertThat(AppEngineWire.currentBrain(wired.registry, wired.prefs).name).isEqualTo("grok")
        wired.prefs.earId = "system"
        wired.prefs.brainId = "none"
        assertThat(AppEngineWire.currentEar(wired.registry, wired.prefs))
            .isSameInstanceAs(wired.system)
        assertThat(AppEngineWire.currentBrain(wired.registry, wired.prefs)).isSameInstanceAs(NoAI)
    }

    @Test
    fun missing_key_still_builds_factory() {
        val wired = wired()
        assertThat(wired.secrets.get("openai")).isNull()
        assertThat(wired.registry.brain("openai")).isInstanceOf(OpenAiCompatBrain::class.java)
        assertThat(wired.registry.ear("openai")).isInstanceOf(OpenAiRealtimeEar::class.java)
    }

    private fun wired(): Wired {
        val system = WireFakeEar("system")
        val registry = ProviderRegistry(fallbackEar = { system }, fallbackBrain = { NoAI })
        val secrets = MemorySecretStore()
        val prefs = EnginePrefs(MemoryPrefsStore()).apply {
            customBaseUrl = "https://example.com/v1"
            brainModel = "test-model"
        }
        val http = CloudHttp { _, _, _ -> "" }
        val socket = CloudSocket { _, _, _ ->
            object : CloudSession {
                override fun send(bytes: ByteArray) {}
                override fun sendText(text: String) {}
                override fun close() {}
            }
        }
        AppEngineWire.install(registry, secrets, prefs, system, http, socket)
        return Wired(registry, secrets, prefs, system)
    }

    private class Wired(
        val registry: ProviderRegistry,
        val secrets: MemorySecretStore,
        val prefs: EnginePrefs,
        val system: SpeechEngine,
    )
}

private class WireFakeEar(private val tag: String) : SpeechEngine {
    override val isAvailable: Boolean = true
    override fun hasMicPermission(): Boolean = true
    override fun setListener(listener: SpeechEngine.Listener?) {}
    override fun startContinuous(languageTag: String) {}
    override fun startOnce(languageTag: String) {}
    override fun stop() {}
    override fun destroy() {}
    override fun toString(): String = "WireFakeEar($tag)"
}
