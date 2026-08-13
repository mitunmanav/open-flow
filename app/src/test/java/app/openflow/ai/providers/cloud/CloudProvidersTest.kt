package app.openflow.ai.providers.cloud

import app.openflow.ai.TextAIProvider
import app.openflow.stt.SpeechEngine
import app.openflow.stt.providers.cloud.CloudSocket
import app.openflow.stt.providers.cloud.CloudSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CloudProvidersTest {

    private class Sink : CloudProviders.Registry {
        val brains = mutableListOf<String>()
        val ears = mutableListOf<String>()
        override fun addBrain(id: String, factory: CloudProviders.BrainFactory) {
            brains += id
        }
        override fun addEar(id: String, factory: CloudProviders.EarFactory) {
            ears += id
        }
    }

    @Test
    fun register_adds_named_brains_and_ears_not_groq() {
        val sink = Sink()
        CloudProviders.register(sink)
        assertThat(sink.brains).containsAtLeast(
            "openai", "grok", "minimax", "deepseek", "gemini",
            "mistral", "together", "fireworks", "openrouter", "sarvam",
            "anthropic", "custom",
        )
        assertThat(sink.brains).doesNotContain("groq")
        assertThat(sink.ears).containsExactly("openai", "deepgram", "assemblyai", "sarvam")
        assertThat(sink.ears).doesNotContain("groq")
        assertThat(sink.ears).doesNotContain("grok")
    }

    @Test
    fun factory_builds_openai_compat_and_anthropic() {
        val http = CloudHttp { _, _, _ -> """{"choices":[{"message":{"content":"x"}}]}""" }
        val openai = CloudProviders.brain("openai", { "k" }, "gpt-4o-mini", null, http)
        val grok = CloudProviders.brain("grok", { "k" }, "grok-3", null, http)
        val custom = CloudProviders.brain("custom", { "k" }, "local", "https://example.com/v1", http)
        val anth = CloudProviders.brain("anthropic", { "k" }, "claude-sonnet-4-20250514", null, http)
        assertThat(openai).isInstanceOf(OpenAiCompatBrain::class.java)
        assertThat(openai!!.name).isEqualTo("openai")
        assertThat(grok).isInstanceOf(OpenAiCompatBrain::class.java)
        assertThat(custom).isInstanceOf(OpenAiCompatBrain::class.java)
        assertThat(anth).isInstanceOf(AnthropicBrain::class.java)
        assertThat(CloudProviders.brain("nope", { "k" }, "m", null, http)).isNull()
    }

    @Test
    fun factory_builds_ears() {
        val sock = CloudSocket { _, _, _ ->
            object : CloudSession {
                override fun send(bytes: ByteArray) {}
                override fun sendText(text: String) {}
                override fun close() {}
            }
        }
        assertThat(CloudProviders.ear("openai", { "k" }, sock)).isInstanceOf(SpeechEngine::class.java)
        assertThat(CloudProviders.ear("deepgram", { "k" }, sock)).isInstanceOf(SpeechEngine::class.java)
        assertThat(CloudProviders.ear("assemblyai", { "k" }, sock)).isInstanceOf(SpeechEngine::class.java)
        assertThat(CloudProviders.ear("sarvam", { "k" }, sock, "translate")).isInstanceOf(SpeechEngine::class.java)
        assertThat(CloudProviders.ear("groq", { "k" }, sock)).isNull()
        assertThat(CloudProviders.ear("grok", { "k" }, sock)).isNull()
    }

    @Test
    fun brains_are_text_ai_providers() {
        val http = CloudHttp { _, _, _ -> "" }
        val b: TextAIProvider = CloudProviders.brain("openai", { "k" }, "m", null, http)!!
        assertThat(b.name).isEqualTo("openai")
    }
}
