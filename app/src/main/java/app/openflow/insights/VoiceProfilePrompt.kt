package app.openflow.insights

data class VoiceFlavor(
    val archetype: String,
    val catchphrase: String,
    val headline: String,
)

/**
 * BYOK Voice flavor: build aggregate-only prompt + parse JSON reply.
 */
object VoiceProfilePrompt {
    fun buildUserMessage(payload: Map<String, Any?>): String {
        val json = payloadToJson(payload)
        return buildString {
            appendLine("You write a short personal dictation voice profile.")
            appendLine("Reply with JSON only — no markdown — keys exactly:")
            appendLine("""{"archetype":"...","catchphrase":"...","headline":"..."}""")
            appendLine("Keep each value under 8 words. English.")
            appendLine("Use only this aggregate data (no transcripts):")
            append(json)
        }
    }

    fun parseFlavor(modelOutput: String): VoiceFlavor? {
        val obj = extractJsonObject(modelOutput) ?: return null
        val archetype = readString(obj, "archetype") ?: return null
        val catchphrase = readString(obj, "catchphrase") ?: return null
        val headline = readString(obj, "headline") ?: return null
        if (archetype.isBlank() || catchphrase.isBlank() || headline.isBlank()) return null
        return VoiceFlavor(archetype.trim(), catchphrase.trim(), headline.trim())
    }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    private fun readString(json: String, key: String): String? {
        val patterns = listOf(
            Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""),
            Regex("\"$key\"\\s*:\\s*'((?:\\\\.|[^'\\\\])*)'"),
        )
        for (p in patterns) {
            val m = p.find(json) ?: continue
            return unescape(m.groupValues[1])
        }
        return null
    }

    private fun unescape(s: String): String =
        s.replace("\\\"", "\"").replace("\\\\", "\\")

    private fun payloadToJson(payload: Map<String, Any?>): String {
        val parts = payload.entries.joinToString(",") { (k, v) ->
            "\"$k\":${valueToJson(v)}"
        }
        return "{$parts}"
    }

    private fun valueToJson(v: Any?): String = when (v) {
        null -> "null"
        is Number -> v.toString()
        is Boolean -> v.toString()
        is String -> "\"${v.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        is List<*> -> v.joinToString(",", "[", "]") { valueToJson(it) }
        is Map<*, *> -> {
            val inner = v.entries.joinToString(",") { (ik, iv) ->
                "\"$ik\":${valueToJson(iv)}"
            }
            "{$inner}"
        }
        else -> "\"${v.toString().replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}
