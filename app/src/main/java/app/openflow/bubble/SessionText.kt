package app.openflow.bubble

/**
 * Pure rules: what text to commit when a dictation listen ends.
 *
 * SpeechRecognizer delivers growing [partial]s and occasional [final]s.
 * Stop often races the last final — we must keep the last partial so
 * "I spoke and tapped stop" still inserts text.
 */
object SessionText {

    /**
     * @param finals joined final segments for this listen (already space-joined)
     * @param lastPartial latest partial hypothesis (may overlap finals)
     */
    fun commitRaw(finals: CharSequence?, lastPartial: CharSequence?): String {
        val f = finals?.toString()?.trim().orEmpty()
        val p = lastPartial?.toString()?.trim().orEmpty()
        if (f.isEmpty() && p.isEmpty()) return ""
        if (p.isEmpty()) return f
        if (f.isEmpty()) return p
        if (f.equals(p, ignoreCase = true)) return f
        val fWords = words(f)
        val pWords = words(p)
        if (fWords.isEmpty()) return p
        if (pWords.isEmpty()) return f
        if (isSuffix(fWords, pWords)) return f
        if (isPrefix(pWords, fWords)) return p
        if (pWords.size == 1 && fWords.any { it.equals(pWords[0], ignoreCase = true) }) return f
        val n = overlapCount(fWords, pWords)
        if (n > 0) {
            val rest = pWords.drop(n).joinToString(" ")
            return if (rest.isEmpty()) f else mergeWithSpace(f, rest)
        }
        return mergeWithSpace(f, p)
    }

    private fun words(s: String): List<String> =
        s.split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun isSuffix(full: List<String>, tail: List<String>): Boolean {
        if (tail.size > full.size) return false
        val start = full.size - tail.size
        return tail.indices.all { i -> full[start + i].equals(tail[i], ignoreCase = true) }
    }

    private fun isPrefix(full: List<String>, head: List<String>): Boolean {
        if (head.size > full.size) return false
        return head.indices.all { i -> full[i].equals(head[i], ignoreCase = true) }
    }

    private fun overlapCount(left: List<String>, right: List<String>): Int {
        val max = minOf(left.size, right.size)
        for (n in max downTo 1) {
            if (isSuffix(left, right.take(n))) return n
        }
        return 0
    }

    private fun mergeWithSpace(base: String, piece: String): String {
        if (piece.isEmpty()) return base
        if (base.isEmpty()) return piece
        val needsSpace = !base.last().isWhitespace() &&
            !piece.first().isWhitespace() &&
            piece.first() !in ".,!?;:\n"
        return if (needsSpace) "$base $piece" else base + piece
    }
}
