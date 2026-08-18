package app.openflow.text

/** Per-sentence question marks. Not a whole-blob scan. English only. */
object QuestionPolicy {
    private val invert2 = setOf(
        "are", "is", "can", "do", "did", "will", "would", "could", "should"
    )
    private val q1 = setOf(
        "who", "what", "where", "when", "why", "how",
        "is", "are", "can", "could", "would", "will", "do", "does", "did"
    )
    private val tagWord = setOf("right", "yeah", "no", "okay")
    private val tagPhrase = listOf("isn't it", "aren't you")

    fun apply(sentence: String): String {
        val raw = sentence.trim()
        if (raw.isEmpty()) return sentence
        val hadEnd = raw.last() in ".!?"
        val core = raw.trimEnd('.', '!', '?').trimEnd()
        if (core.isEmpty()) return sentence
        val low = core.lowercase()
        if (tagPhrase.any { low.endsWith(it) }) return "$core?"
        val words = core.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return sentence
        val lastBare = words.last().trimEnd(',', ';').lowercase()
        if (lastBare in tagWord) return "$core?"
        val first = words.first().lowercase().trimEnd(',', ';')
        val second = words.getOrNull(1)?.lowercase()?.trimEnd(',', ';').orEmpty()
        val qStart = first in q1
        val invert = second in invert2
        if (qStart && invert) return "$core?"
        if (qStart && !invert && hadEnd && raw.endsWith(".") && words.size >= 6) {
            return raw
        }
        if (qStart && words.size <= 12) return "$core?"
        return if (hadEnd) raw else core
    }

    fun applyAll(text: String): String {
        if (text.isBlank()) return text
        val parts = ArrayList<String>()
        val buf = StringBuilder()
        fun flush() {
            if (buf.isEmpty()) return
            parts += apply(buf.toString())
            buf.setLength(0)
        }
        var i = 0
        while (i < text.length) {
            val c = text[i]
            buf.append(c)
            if (c == '.' || c == '!' || c == '?') {
                flush()
                while (i + 1 < text.length && text[i + 1].isWhitespace()) {
                    i++
                    buf.append(text[i])
                }
                if (buf.isNotEmpty()) {
                    parts += buf.toString()
                    buf.setLength(0)
                }
            }
            i++
        }
        flush()
        return parts.joinToString("").replace(Regex(" +"), " ").trim()
    }
}
