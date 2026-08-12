package app.openflow.text

/**
 * Spoken control words → symbols / edit ops (Wispr Smart Formatting, local).
 *
 * Data: assets/voice_commands.json (classpath copy for JVM tests).
 * Engine: tokenize → longest-phrase map walk (FSM). Not regex soup.
 *
 * Applied on Light+ cleanup. None (RAW) keeps words as spoken.
 */
object VoiceCommands {

    private val map: PhraseMap get() = PhraseMap.default

    fun apply(raw: String): String = apply(raw, map)

    /** Testable entry with explicit map. */
    fun apply(raw: String, phraseMap: PhraseMap): String {
        if (raw.isBlank()) return raw
        val tokens = tokenize(raw)
        if (tokens.isEmpty()) return raw

        val out = StringBuilder()
        var i = 0
        while (i < tokens.size) {
            when (val m = phraseMap.matchAt(tokens, i)) {
                is PhraseMap.Match.Insert -> {
                    out.append(' ').append(m.symbol).append(' ')
                    i += m.wordCount
                }
                is PhraseMap.Match.Edit -> {
                    applyEdit(out, m.op)
                    i += m.wordCount
                }
                null -> {
                    if (out.isNotEmpty() && !out.last().isWhitespace()) out.append(' ')
                    out.append(tokens[i])
                    i++
                }
            }
        }
        return tidy(out.toString())
    }

    private fun tokenize(raw: String): List<String> =
        raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun applyEdit(out: StringBuilder, op: PhraseMap.EditOp) {
        val left = out.toString()
        val next = when (op) {
            PhraseMap.EditOp.WORD -> dropLastWord(left)
            PhraseMap.EditOp.CHAR -> dropLastChar(left)
            PhraseMap.EditOp.SENTENCE -> dropLastSentence(left)
            PhraseMap.EditOp.CLEAR -> ""
        }
        out.setLength(0)
        out.append(next)
    }

    internal fun dropLastWord(left: String): String {
        val t = left.trimEnd()
        if (t.isEmpty()) return ""
        val i = t.indexOfLast { it.isWhitespace() }
        return if (i < 0) "" else t.substring(0, i).trimEnd() + " "
    }

    internal fun dropLastChar(left: String): String {
        val t = left.trimEnd()
        if (t.isEmpty()) return ""
        return t.dropLast(1)
    }

    internal fun dropLastSentence(left: String): String {
        val t = left.trimEnd()
        if (t.isEmpty()) return ""
        var i = t.length - 1
        while (i >= 0 && t[i] in ".!?") i--
        while (i >= 0 && t[i].isWhitespace()) i--
        while (i >= 0 && t[i] !in ".!?\n") i--
        return if (i < 0) "" else t.substring(0, i + 1).trimEnd() + " "
    }

    private fun tidy(s: String): String {
        var t = s
        t = t.replace(Regex("""\s+([.,!?;:)\]])"""), "$1")
        t = t.replace(Regex("""([(\[])\s+"""), "$1")
        t = t.replace(Regex("""([.!?])([A-Za-z])"""), "$1 $2")
        t = t.replace(Regex("""(,)([A-Za-z])"""), "$1 $2")
        t = t.lines().joinToString("\n") { line ->
            line.replace(Regex("[ \\t]+"), " ").trim()
        }
        t = t.replace(Regex("\n{3,}"), "\n\n")
        return t.trim()
    }
}
