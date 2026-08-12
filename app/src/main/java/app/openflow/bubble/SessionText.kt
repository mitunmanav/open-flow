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
        // Partial already fully contained as suffix / whole
        if (f == p || f.endsWith(p) || f.contains(p)) return f
        // Growing hypothesis supersedes shorter final blob
        if (p.startsWith(f)) return p
        return mergeWithSpace(f, p)
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
