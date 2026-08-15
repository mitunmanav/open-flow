package app.openflow.engine

object ProviderNotice {
    fun line(earId: String, brainId: String): String {
        val ear = ProviderId.parseEar(earId)
        val brain = ProviderId.parseBrain(brainId)
        return when {
            ear == EarId.LAPTOP || brain == BrainId.LAPTOP ->
                "Audio/text goes to the computer you set."
            brain == BrainId.GROK ->
                "Text of this utterance goes to xAI (Grok). Not Groq."
            ear == EarId.OPENAI ->
                "Your voice goes to OpenAI."
            ear == EarId.ON_PHONE && brain == BrainId.NONE ->
                "Audio stays on this phone."
            ear == EarId.SYSTEM && brain == BrainId.NONE ->
                "On this phone. Phone speech may still use Google."
            else -> cloudLine(ear, brain)
        }
    }

    private fun cloudLine(ear: EarId, brain: BrainId): String {
        val parts = mutableListOf<String>()
        if (ear != EarId.SYSTEM && ear != EarId.ON_PHONE) {
            parts += "Your voice goes to ${vendorName(ear.name)}."
        }
        if (brain != BrainId.NONE && brain != BrainId.ON_PHONE) {
            parts += "Text of this utterance goes to ${vendorName(brain.name)}."
        }
        return parts.joinToString(" ").ifEmpty {
            "On this phone. Phone speech may still use Google."
        }
    }

    private fun vendorName(raw: String): String = when (raw) {
        "OPENAI" -> "OpenAI"
        "DEEPGRAM" -> "Deepgram"
        "ASSEMBLYAI" -> "AssemblyAI"
        "SARVAM" -> "Sarvam"
        "MINIMAX" -> "MiniMax"
        "DEEPSEEK" -> "DeepSeek"
        "GEMINI" -> "Gemini"
        "MISTRAL" -> "Mistral"
        "TOGETHER" -> "Together"
        "FIREWORKS" -> "Fireworks"
        "OPENROUTER" -> "OpenRouter"
        "ANTHROPIC" -> "Anthropic"
        "CUSTOM", "CUSTOM_STT" -> "the endpoint you set"
        else -> raw.lowercase()
    }
}
