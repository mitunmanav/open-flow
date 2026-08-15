package app.openflow.ui.copy

/**
 * UI label casing. Material/Google: sentence case for controls.
 * Proper nouns stay capitalized.
 */
object UiCopy {
    private val proper = setOf(
        "AI", "API", "STT", "URL", "OEM", "OS", "FTS", "CVV", "OTP",
        "OpenAI", "Open", "Flow", "Deepgram", "AssemblyAI", "Sarvam",
        "Anthropic", "Gemini", "Mistral", "DeepSeek", "MiniMax", "Grok",
        "OpenRouter", "Fireworks", "Together", "PhonePe", "Paytm",
        "WhatsApp", "Gboard",
    )

    fun sentenceLabel(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        val parts = t.split(Regex("\\s+"))
        return parts.mapIndexed { i, word ->
            val bare = word.trimStart('(', '[').trimEnd(')', ']', ',', '.', ':', ';')
            val prefix = word.takeWhile { it == '(' || it == '[' }
            val suffix = word.takeLastWhile { it == ')' || it == ']' || it == ',' || it == '.' || it == ':' || it == ';' }
            val core = word.removePrefix(prefix).removeSuffix(suffix)
            when {
                i == 0 -> keepFirst(core)
                isProper(bare) || isProper(core) -> core
                core.contains('+') -> core // Speech + AI
                else -> core.replaceFirstChar { ch -> ch.lowercaseChar() }
            }.let { prefix + it + suffix }
        }.joinToString(" ")
    }

    private fun keepFirst(core: String): String {
        if (core.isEmpty()) return core
        if (isProper(core)) return core
        return core.replaceFirstChar { it.uppercaseChar() }
    }

    private fun isProper(token: String): Boolean {
        if (token.isEmpty()) return false
        if (token in proper) return true
        // multi-token proper handled per word; OpenAI-style
        return proper.any { it.equals(token, ignoreCase = false) && it == token }
    }
}
