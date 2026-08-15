package app.openflow.ai.providers.cloud

import app.openflow.ai.TextAIProvider
import app.openflow.stt.SpeechEngine
import app.openflow.stt.providers.cloud.AssemblyEar
import app.openflow.stt.providers.cloud.CloudSocket
import app.openflow.stt.providers.cloud.DeepgramEar
import app.openflow.stt.providers.cloud.OpenAiRealtimeEar
import app.openflow.stt.providers.cloud.PcmSource
import app.openflow.stt.providers.cloud.SarvamEar

object CloudProviders {

    fun interface BrainFactory {
        fun create(
            apiKey: () -> String,
            model: String,
            baseUrl: String?,
            http: CloudHttp,
        ): TextAIProvider
    }

    fun interface EarFactory {
        fun create(
            apiKey: () -> String,
            socket: CloudSocket,
            mode: String,
            pcm: PcmSource,
        ): SpeechEngine
    }

    interface Registry {
        fun addBrain(id: String, factory: BrainFactory)
        fun addEar(id: String, factory: EarFactory)
    }

    private val namedBrains = listOf(
        "openai", "grok", "minimax", "deepseek", "gemini",
        "mistral", "together", "fireworks", "openrouter", "sarvam",
    )

    fun register(registry: Registry) {
        for (id in namedBrains) {
            registry.addBrain(id) { key, model, url, http ->
                brain(id, key, model, url, http)!!
            }
        }
        registry.addBrain("anthropic") { key, model, _, http ->
            brain("anthropic", key, model, null, http)!!
        }
        registry.addBrain("custom") { key, model, url, http ->
            brain("custom", key, model, url, http)!!
        }
        registry.addEar("openai") { key, socket, _, pcm -> ear("openai", key, socket, pcm = pcm)!! }
        registry.addEar("deepgram") { key, socket, _, pcm -> ear("deepgram", key, socket, pcm = pcm)!! }
        registry.addEar("assemblyai") { key, socket, _, pcm -> ear("assemblyai", key, socket, pcm = pcm)!! }
        registry.addEar("sarvam") { key, socket, mode, pcm -> ear("sarvam", key, socket, mode, pcm)!! }
    }

    fun brain(
        id: String,
        apiKey: () -> String,
        model: String,
        baseUrl: String?,
        http: CloudHttp,
    ): TextAIProvider? {
        if (id == "anthropic") return AnthropicBrain(apiKey, model, http)
        val url = baseUrl?.takeIf { it.isNotBlank() } ?: NamedCloud.brainBaseUrl(id) ?: return null
        return OpenAiCompatBrain(id, apiKey, model, url, http)
    }

    fun ear(
        id: String,
        apiKey: () -> String,
        socket: CloudSocket,
        mode: String = "transcribe",
        pcm: PcmSource = PcmSource.None,
    ): SpeechEngine? = when (id) {
        "openai" -> OpenAiRealtimeEar(apiKey, socket, pcm = pcm)
        "deepgram" -> DeepgramEar(apiKey, socket, pcm = pcm)
        "assemblyai" -> AssemblyEar(apiKey, socket, pcm = pcm)
        "sarvam" -> SarvamEar(apiKey, socket, mode, pcm = pcm)
        else -> null
    }
}
