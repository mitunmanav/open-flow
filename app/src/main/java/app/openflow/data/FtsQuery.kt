package app.openflow.data

/**
 * Sanitize user search into a safe FTS4 MATCH expression.
 * Strips operators so MATCH never throws on `" * ( ) : ^` / AND|OR|NOT.
 * Returns null when nothing searchable remains (caller lists all).
 */
object FtsQuery {
    private val reserved = setOf("AND", "OR", "NOT", "NEAR")
    private val noise = Regex("""["*():^]""")

    fun sanitize(raw: String): String? {
        val cleaned = noise.replace(raw.trim(), " ")
        val tokens = cleaned
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it.uppercase() !in reserved }
            .filter { it.any { ch -> ch.isLetterOrDigit() } }
            .map { "$it*" }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ")
    }
}
