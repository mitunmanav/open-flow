package app.openflow.ai.providers.cloud

/** Named BYOK brain base URLs. Grok = xAI, not Groq. Custom is user URL. */
object NamedCloud {
    fun brainBaseUrl(id: String): String? = when (id) {
        "openai" -> "https://api.openai.com/v1"
        "grok" -> "https://api.x.ai/v1" // xAI Grok — not Groq
        "minimax" -> "https://api.minimax.io/v1"
        "deepseek" -> "https://api.deepseek.com"
        "gemini" -> "https://generativelanguage.googleapis.com/v1beta/openai/"
        "mistral" -> "https://api.mistral.ai/v1"
        "together" -> "https://api.together.xyz/v1"
        "fireworks" -> "https://api.fireworks.ai/inference/v1"
        "openrouter" -> "https://openrouter.ai/api/v1"
        "sarvam" -> "https://api.sarvam.ai/v1"
        else -> null
    }
}
