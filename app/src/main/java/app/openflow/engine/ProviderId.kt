package app.openflow.engine

enum class EarId {
    SYSTEM,
    ON_PHONE,
    LAPTOP,
    OPENAI,
    DEEPGRAM,
    ASSEMBLYAI,
    SARVAM,
    CUSTOM_STT,
}

enum class BrainId {
    NONE,
    ON_PHONE,
    LAPTOP,
    OPENAI,
    GROK,
    MINIMAX,
    DEEPSEEK,
    GEMINI,
    MISTRAL,
    TOGETHER,
    FIREWORKS,
    OPENROUTER,
    SARVAM,
    ANTHROPIC,
    CUSTOM,
}

object ProviderId {
    fun parseEar(id: String): EarId = when (id.lowercase()) {
        "system" -> EarId.SYSTEM
        "on_phone" -> EarId.ON_PHONE
        "laptop" -> EarId.LAPTOP
        "openai" -> EarId.OPENAI
        "deepgram" -> EarId.DEEPGRAM
        "assemblyai" -> EarId.ASSEMBLYAI
        "sarvam" -> EarId.SARVAM
        "custom_stt" -> EarId.CUSTOM_STT
        else -> EarId.SYSTEM
    }

    fun parseBrain(id: String): BrainId = when (id.lowercase()) {
        "none" -> BrainId.NONE
        "on_phone" -> BrainId.ON_PHONE
        "laptop" -> BrainId.LAPTOP
        "openai" -> BrainId.OPENAI
        "grok" -> BrainId.GROK
        "minimax" -> BrainId.MINIMAX
        "deepseek" -> BrainId.DEEPSEEK
        "gemini" -> BrainId.GEMINI
        "mistral" -> BrainId.MISTRAL
        "together" -> BrainId.TOGETHER
        "fireworks" -> BrainId.FIREWORKS
        "openrouter" -> BrainId.OPENROUTER
        "sarvam" -> BrainId.SARVAM
        "anthropic" -> BrainId.ANTHROPIC
        "custom" -> BrainId.CUSTOM
        else -> BrainId.NONE
    }
}
