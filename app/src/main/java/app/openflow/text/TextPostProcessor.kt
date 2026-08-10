package app.openflow.text

/**
 * Local post-process to approximate Wispr cleanup without cloud.
 * Prefer [CleanupPipeline] for session polish; [process] remains for light unit use.
 * Full session is polished once on stop (not every raw STT chunk into the field).
 */
object TextPostProcessor {

    // "i mean" is a course-correct marker — handled by CourseCorrector, not stripped here
    private val fillers = listOf(
        "um", "uh", "erm", "ah", "uhm", "hmm", "you know", "sort of", "kind of"
    )

    /**
     * Full production polish for one dictation session.
     * Thin wrap over [CleanupPipeline]; [courseCorrect] maps light vs normal/high.
     */
    fun polishSession(raw: String, style: Style = Style.CASUAL, courseCorrect: Boolean = true): String =
        polishSessionResult(
            raw = raw,
            style = style,
            level = if (courseCorrect) CleanupLevel.NORMAL else CleanupLevel.LIGHT
        ).clean

    /** Dual raw/clean result for stop path + history. */
    fun polishSessionResult(
        raw: String,
        style: Style = Style.CASUAL,
        level: CleanupLevel = CleanupLevel.NORMAL
    ): CleanupResult = CleanupPipeline.run(raw, level, style)

    fun process(raw: String, style: Style = Style.CASUAL): String {
        if (raw.isBlank()) return raw
        var t = raw.trim()
        t = stripFillers(t)
        t = normalizeSpaces(t)
        t = applyVoiceCommands(t)
        t = applyListHints(t)
        t = applyPunctuation(t, style)
        t = applyCapitalization(t)
        return t.trim()
    }

    fun applyDictionary(text: String, replacements: Map<String, String>): String {
        if (replacements.isEmpty()) return text
        var out = text
        // Longer keys first
        replacements.entries.sortedByDescending { it.key.length }.forEach { (from, to) ->
            if (from.isBlank()) return@forEach
            val regex = Regex("\\b${Regex.escape(from)}\\b", RegexOption.IGNORE_CASE)
            out = regex.replace(out) { to }
        }
        return out
    }

    fun expandSnippets(text: String, snippets: Map<String, String>): String {
        if (snippets.isEmpty()) return text
        var out = text.trim()
        snippets.entries.sortedByDescending { it.key.length }.forEach { (trigger, body) ->
            if (trigger.isBlank()) return@forEach
            // exact whole utterance match (Wispr voice trigger style)
            if (out.equals(trigger, ignoreCase = true)) {
                out = body
            }
        }
        return out
    }

    private fun stripFillers(t: String): String {
        var out = t
        fillers.sortedByDescending { it.length }.forEach { f ->
            out = out.replace(Regex("\\b${Regex.escape(f)}\\b[,\\s]*", RegexOption.IGNORE_CASE), " ")
        }
        // standalone "like" only when filler (surrounded by pauses / not "I like X")
        out = out.replace(Regex("""(?i)(?<=\s|^)like(?=\s)(?!\s+(?:to|a|an|the|my|your|this|that)\b)"""), " ")
        return normalizeSpaces(out)
    }

    private fun normalizeSpaces(t: String) = t.replace(Regex("\\s+"), " ").trim()

    /** Spoken layout commands → real newlines / punctuation tokens. */
    private fun applyVoiceCommands(t: String): String {
        var s = t
        s = s.replace(Regex("""(?i)\bnew\s+paragraph\b"""), "\n\n")
        s = s.replace(Regex("""(?i)\bnew\s+line\b"""), "\n")
        s = s.replace(Regex("""(?i)\bperiod\b"""), ".")
        s = s.replace(Regex("""(?i)\bcomma\b"""), ",")
        s = s.replace(Regex("""(?i)\bquestion\s+mark\b"""), "?")
        s = s.replace(Regex("""(?i)\bexclamation\s+(?:mark|point)\b"""), "!")
        // tidy spaces before punctuation
        s = s.replace(Regex("""\s+([.,!?])"""), "$1")
        return s
    }

    private fun applyListHints(t: String): String {
        // "number one X number two Y" -> "1. X\n2. Y"
        val m = Regex(
            "(?i)(?:number|item)\\s+(one|two|three|four|five|1|2|3|4|5)\\s+",
        )
        if (!m.containsMatchIn(t)) return t
        val parts = t.split(Regex("(?i)(?:number|item)\\s+(?:one|two|three|four|five|1|2|3|4|5)\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size < 2) return t
        return parts.mapIndexed { i, p -> "${i + 1}. ${p.trim().trimEnd('.', ',')}" }.joinToString("\n")
    }

    private fun applyPunctuation(t: String, style: Style): String {
        var s = t
        // sentence end if missing
        if (s.lastOrNull()?.isLetterOrDigit() == true) {
            s += when (style) {
                Style.FORMAL -> "."
                Style.EXCITED -> "!"
                else -> if (s.length > 40) "." else ""
            }
        }
        // comma-ish pauses already gone; ensure question
        if (s.startsWith("who ", true) || s.startsWith("what ", true) ||
            s.startsWith("where ", true) || s.startsWith("when ", true) ||
            s.startsWith("why ", true) || s.startsWith("how ", true) ||
            s.startsWith("is ", true) || s.startsWith("are ", true) ||
            s.startsWith("can ", true) || s.startsWith("do ", true)
        ) {
            if (!s.endsWith("?")) {
                s = s.trimEnd('.', '!') + "?"
            }
        }
        return s
    }

    private fun applyCapitalization(t: String): String {
        if (t.isEmpty()) return t
        val sb = StringBuilder(t)
        sb[0] = sb[0].uppercaseChar()
        // after . ! ? newline
        var i = 1
        while (i < sb.length) {
            val c = sb[i - 1]
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                var j = i
                while (j < sb.length && sb[j].isWhitespace()) j++
                if (j < sb.length && sb[j].isLowerCase()) {
                    sb[j] = sb[j].uppercaseChar()
                }
            }
            i++
        }
        return sb.toString()
    }

    enum class Style { CASUAL, FORMAL, EXCITED }
}

