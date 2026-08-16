package app.openflow.text

/**
 * Utterance-only spell hints for a picked brain.
 * Never dump the full dictionary or history to the network.
 */
object LearnPrompt {
    fun utteranceHints(text: String, dictionary: Map<String, String>): String {
        if (text.isBlank() || dictionary.isEmpty()) return ""
        return dictionary.entries
            .filter { it.key.isNotBlank() && text.contains(it.key, ignoreCase = true) }
            .joinToString("; ") { "${it.key.lowercase()}→${it.value}" }
    }
}
