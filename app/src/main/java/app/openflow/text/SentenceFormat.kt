package app.openflow.text

/**
 * Deterministic sentence-start caps. No ICU [BreakIterator] — it splits at
 * digit+space boundaries and mid-capitalizes ITN output ("15 percent").
 */
object SentenceFormat {

    /** Capitalize first letter of each sentence. No dictionary. */
    fun capitalizeSentences(text: String): String {
        if (text.isEmpty()) return text
        val out = StringBuilder(text)
        // First letter of the whole text.
        for (i in out.indices) {
            if (out[i].isLetter()) {
                if (out[i].isLowerCase()) out.setCharAt(i, out[i].uppercaseChar())
                break
            }
            if (!out[i].isWhitespace()) break
        }
        return capAfterNewlines(capAfterTerminators(out.toString()))
    }

    /** Cap the first letter of each new line (spoken "new line"). */
    private fun capAfterNewlines(text: String): String {
        val out = StringBuilder(text)
        var i = 0
        while (i < out.length) {
            if (out[i] == '\n') {
                var j = i + 1
                while (j < out.length && (out[j] == ' ' || out[j] == '\t')) j++
                if (j < out.length && out[j].isLetter() && out[j].isLowerCase()) {
                    out.setCharAt(j, out[j].uppercaseChar())
                }
            }
            i++
        }
        return out.toString()
    }

    /** Cap the first letter after . ! ? followed by whitespace/EOL. */
    private fun capAfterTerminators(text: String): String {
        val out = StringBuilder(text)
        var i = 0
        while (i < out.length) {
            val c = out[i]
            if (c == '.' || c == '!' || c == '?') {
                var j = i + 1
                while (j < out.length && out[j].isWhitespace()) j++
                if (j < out.length && j > i + 1 &&
                    out[j].isLetter() && out[j].isLowerCase()
                ) {
                    out.setCharAt(j, out[j].uppercaseChar())
                }
            }
            i++
        }
        return out.toString()
    }
}
