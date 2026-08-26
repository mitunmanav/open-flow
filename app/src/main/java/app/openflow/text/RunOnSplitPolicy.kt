package app.openflow.text

/** Closed-bridge run-on split. English. Never drop words. */
object RunOnSplitPolicy {
    private val ws = Regex("\\s+")
    private val bridge = Regex(
        """(?i)(?<=\s)((?:and then|then|so|but)\s+(?:I|we|they))(?=\s)"""
    )
    private val timeThenClause = Regex(
        """(?i)\b(noon|midnight|morning|evening|tonight|today|yesterday|tomorrow)\s+(it|i|we|they|he|she)\b"""
    )

    fun apply(t: String): String {
        val s = t.trim()
        if (s.isEmpty()) return t
        if (s.contains(Regex("""^\d+\.""")) || s.contains(Regex("""(?i)\d+\.\s"""))) {
            return t
        }
        timeThenClause.find(s)?.let { m ->
            val time = m.groupValues[1]
            val pronoun = m.groupValues[2]
            val left = s.substring(0, m.range.first + time.length).trim()
            val right = s.substring(m.range.first + time.length).trim()
            val last = left.lastOrNull()
            if (wordCount(left) >= 3 && wordCount(right) >= 3 &&
                last != '.' && last != '?' && last != '!'
            ) {
                val cap = pronoun.replaceFirstChar { ch ->
                    if (ch.isLetter()) ch.uppercaseChar() else ch
                }
                val rest = right.drop(pronoun.length).trim()
                return "$left. $cap $rest"
            }
        }
        val hits = bridge.findAll(s).toList()
        if (hits.size != 1) return t
        val m = hits.first()
        val left = s.substring(0, m.range.first).trim()
        val right = s.substring(m.range.last + 1).trim()
        if (wordCount(left) < 4 || wordCount(right) < 4) return t
        if (left.lastOrNull() == '.' || left.lastOrNull() == '?' || left.lastOrNull() == '!') {
            return t
        }
        val pronoun = m.value.split(ws).last()
        val capPronoun = pronoun.replaceFirstChar { ch ->
            if (ch.isLetter()) ch.uppercaseChar() else ch
        }
        return "$left. $capPronoun $right"
    }

    private fun wordCount(s: String): Int =
        s.split(ws).count { it.isNotEmpty() }
}
