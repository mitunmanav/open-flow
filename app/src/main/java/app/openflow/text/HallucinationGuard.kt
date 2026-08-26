package app.openflow.text

/**
 * Whisper-path hallucination guard (T17).
 * Collapses repeated 4-grams. Signature phrases are flagged and kept —
 * never silently deleted.
 */
object HallucinationGuard {

    data class Result(
        val text: String,
        val loopCollapsed: Boolean,
        val signatures: List<String>,
    )

    /** Common English Whisper silence / YouTube-ending hallucinations. */
    internal val signaturePhrases = listOf(
        "thanks for watching",
        "thank you for watching",
        "please like and subscribe",
        "like and subscribe",
        "don't forget to subscribe",
        "smash that like button",
        "see you in the next video",
        "thanks for listening",
        "this video is sponsored",
        "leave a comment below",
        "thanks for tuning in",
        "please subscribe",
    )

    fun apply(text: String): Result {
        if (text.isBlank()) return Result(text, false, emptyList())
        val collapsed = collapseLoops(text)
        val low = collapsed.lowercase()
        val found = signaturePhrases.filter { p ->
            val re = Regex("""(?<![A-Za-z])${Regex.escape(p)}(?![A-Za-z])""", RegexOption.IGNORE_CASE)
            re.containsMatchIn(low)
        }
        return Result(collapsed, collapsed != normalizeWs(text), found)
    }

    /** Collapse consecutive identical 4-grams to a single copy. */
    internal fun collapseLoops(text: String): String {
        val tokens = text.trim().split(ws).filter { it.isNotEmpty() }
        if (tokens.size < 8) return normalizeWs(text)
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            if (i + 8 <= tokens.size) {
                val gram = tokens.subList(i, i + 4)
                var j = i + 4
                var repeats = 1
                while (j + 4 <= tokens.size && sameGram(gram, tokens.subList(j, j + 4))) {
                    repeats++
                    j += 4
                }
                if (repeats >= 2) {
                    out.addAll(gram)
                    i = j
                    continue
                }
            }
            out.add(tokens[i])
            i++
        }
        return out.joinToString(" ")
    }

    private fun sameGram(a: List<String>, b: List<String>): Boolean {
        if (a.size != b.size) return false
        for (k in a.indices) {
            if (a[k].lowercase() != b[k].lowercase()) return false
        }
        return true
    }

    private fun normalizeWs(t: String): String = t.trim().replace(ws, " ")

    private val ws = Regex("\\s+")
}
