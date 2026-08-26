package app.openflow.text

/**
 * Mid-sentence continuation (T16). When the field already has text that is not
 * a finished sentence, the new blob joins in lowercase. Closed prefixes
 * (. ! ? or a newline) keep the spoken casing.
 */
object ContinuationPolicy {

    private val closed = setOf('.', '!', '?', '\n')
    private val noSpaceBefore = setOf('.', ',', '!', '?', ';', ':', '\n')

    fun isOpen(prefix: String): Boolean {
        val t = prefix.trimEnd(' ', '\t')
        if (t.isEmpty()) return false
        return t.last() !in closed
    }

    fun join(prefix: String, spoken: String): String {
        val base = prefix
        val piece = spoken.trim()
        if (piece.isEmpty()) return base
        if (base.isEmpty()) return piece
        val head = base.trim()
        if (head.isNotEmpty() && piece.startsWith(head, ignoreCase = true)) return piece
        val adjusted = if (isOpen(base)) downcaseLead(piece) else piece
        val needsSpace = !base.last().isWhitespace() &&
            adjusted.isNotEmpty() &&
            !adjusted.first().isWhitespace() &&
            adjusted.first() !in noSpaceBefore
        return if (needsSpace) "$base $adjusted" else base + adjusted
    }

    private fun downcaseLead(s: String): String {
        val i = s.indexOfFirst { it.isLetter() }
        if (i < 0) return s
        val wordEnd = s.indexOfFirst { it.isWhitespace() }.let { if (it < 0) s.length else it }
        val firstWord = s.substring(i, wordEnd)
        val letters = firstWord.filter { it.isLetter() }
        if (letters.length >= 2 && letters.all { it.isUpperCase() }) return s
        return s.substring(0, i) + s[i].lowercaseChar() + s.substring(i + 1)
    }
}
