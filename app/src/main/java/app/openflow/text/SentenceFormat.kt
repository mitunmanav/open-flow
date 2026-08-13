package app.openflow.text

import java.text.BreakIterator
import java.util.Locale

/**
 * Sentence-start caps via ICU [BreakIterator]. No word lists.
 */
object SentenceFormat {

    /** Capitalize first letter of each sentence. No dictionary. */
    fun capitalizeSentences(text: String): String {
        if (text.isEmpty()) return text
        val boundary = BreakIterator.getSentenceInstance(Locale.US)
        boundary.setText(text)
        val out = StringBuilder(text.length)
        var start = boundary.first()
        var end = boundary.next()
        while (end != BreakIterator.DONE) {
            out.append(capFirstLetter(text.substring(start, end)))
            start = end
            end = boundary.next()
        }
        // Host ICU often will not break on ". lowercase" (STT is usually lower).
        return capAfterTerminators(out.toString())
    }

    /** Cap the first letter after . ! ? — not a word list. */
    private fun capAfterTerminators(text: String): String {
        val out = StringBuilder(text)
        var i = 0
        while (i < out.length) {
            val c = out[i]
            if (c == '.' || c == '!' || c == '?') {
                var j = i + 1
                while (j < out.length && out[j].isWhitespace()) j++
                if (j < out.length && out[j].isLetter() && out[j].isLowerCase()) {
                    out.setCharAt(j, out[j].uppercaseChar())
                }
            }
            i++
        }
        return out.toString()
    }

    private fun capFirstLetter(sentence: String): String {
        val i = sentence.indexOfFirst { it.isLetter() }
        if (i < 0) return sentence
        val c = sentence[i]
        if (c.isUpperCase()) return sentence
        return buildString(sentence.length) {
            append(sentence, 0, i)
            append(c.uppercaseChar())
            append(sentence, i + 1, sentence.length)
        }
    }
}
