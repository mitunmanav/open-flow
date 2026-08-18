package app.openflow.text

/** Closed-bridge run-on split. English. Never drop words. */
object RunOnSplitPolicy {
    private val ws = Regex("\\s+")
    private val bridge = Regex(
        """(?i)(?<=\s)((?:and then|then|so|but)\s+(?:I|we|they))(?=\s)"""
    )

    fun apply(t: String): String {
        val s = t.trim()
        if (s.isEmpty()) return t
        if (s.contains(Regex("""^\d+\.""")) || s.contains(Regex("""(?i)\d+\.\s"""))) {
            return t
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
