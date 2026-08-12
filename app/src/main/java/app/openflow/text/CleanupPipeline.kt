package app.openflow.text

/**
 * Staged local cleanup. Deterministic first; no LLM required.
 *
 * Raw STT → normalize → fillers → repetitions → false starts
 * → self-corrections → punctuation → capitalization → verify
 */
object CleanupPipeline {

    fun run(
        raw: String,
        level: CleanupLevel = CleanupLevel.NORMAL,
        style: TextPostProcessor.Style = TextPostProcessor.Style.CASUAL
    ): CleanupResult {
        val original = raw.trim()
        if (original.isEmpty()) {
            return CleanupResult(raw = "", clean = "", level = level)
        }
        if (level == CleanupLevel.RAW) {
            return CleanupResult(raw = original, clean = original, level = level)
        }

        var t = normalize(original)
        t = stripFillers(t)
        t = collapseRepetitions(t)
        t = stripFalseStarts(t)

        val corrections = mutableListOf<Correction>()
        if (level == CleanupLevel.NORMAL || level == CleanupLevel.HIGH) {
            val analyzed = CourseCorrector.analyze(t)
            corrections += analyzed.corrections
            t = analyzed.text
        }

        t = applySpokenPunctuation(t)
        t = applyListHints(t)
        t = applyPunctuation(t, style)
        t = applyCapitalization(t)
        t = normalize(t)

        return CleanupResult(
            raw = original,
            clean = t,
            corrections = corrections,
            level = level
        )
    }

    // --- stages (testable individually via package-visible helpers) ---

    internal fun normalize(t: String): String =
        t.replace(Regex("\\s+"), " ").trim()

    private val fillers = listOf(
        "um", "uh", "erm", "ah", "uhm", "hmm", "you know", "sort of", "kind of"
    )

    internal fun stripFillers(t: String): String {
        var out = t
        fillers.sortedByDescending { it.length }.forEach { f ->
            out = out.replace(
                Regex("\\b${Regex.escape(f)}\\b[,\\s]*", RegexOption.IGNORE_CASE),
                " "
            )
        }
        // Collapse empty comma slots left by filler removal: "I, , like," → "I, like,"
        out = out.replace(Regex("""\s*,\s*,+"""), ",")
        // Spoken filler ", like," → keep verb "like" as content ("I like pizza")
        out = out.replace(Regex("""(?i),\s*like\s*,"""), " like ")
        // Drop filler "like" only when followed by another filler / trailing alone
        out = out.replace(
            Regex("""(?i)(?<=\s)like(?=\s+(?:um|uh|you know)\b)"""),
            " "
        )
        out = out.replace(Regex("""\s*,\s*,+"""), ",")
        out = out.replace(Regex("""\s+,"""), ",")
        return normalize(out)
    }

    /** Collapse immediate repeated words: "I I want" → "I want". */
    internal fun collapseRepetitions(t: String): String {
        val words = t.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 2) return t
        val out = ArrayList<String>(words.size)
        for (w in words) {
            val prev = out.lastOrNull()
            if (prev != null && prev.equals(w, ignoreCase = true)) continue
            out.add(w)
        }
        // short phrase repeats: "go to go to"
        var s = out.joinToString(" ")
        s = s.replace(
            Regex("""(?i)\b(\w+(?:\s+\w+){0,2})\s+\1\b"""),
            "$1"
        )
        return normalize(s)
    }

    /**
     * Lightweight false-start: "I was going to — I will go" → take after dash/ellipsis restart.
     */
    internal fun stripFalseStarts(t: String): String {
        var s = t
        // "I want to... start over" style already in CourseCorrector (scratch that)
        s = s.replace(Regex("""(?i)\bI\s+was\s+going\s+to\b[^.]*?[—–-]\s*"""), "")
        return normalize(s)
    }

    private fun applySpokenPunctuation(t: String): String {
        var s = t
        s = s.replace(Regex("""(?i)\bnew\s+paragraph\b"""), "\n\n")
        s = s.replace(Regex("""(?i)\bnew\s+line\b"""), "\n")
        s = s.replace(Regex("""(?i)\bperiod\b"""), ".")
        s = s.replace(Regex("""(?i)\bcomma\b"""), ",")
        s = s.replace(Regex("""(?i)\bquestion\s+mark\b"""), "?")
        s = s.replace(Regex("""(?i)\bexclamation\s+(?:mark|point)\b"""), "!")
        s = s.replace(Regex("""(?i)\bcolon\b"""), ":")
        s = s.replace(Regex("""(?i)\bsemicolon\b"""), ";")
        s = s.replace(Regex("""\s+([.,!?;:])"""), "$1")
        return s
    }

    private fun applyListHints(t: String): String {
        val m = Regex("(?i)(?:number|item)\\s+(one|two|three|four|five|1|2|3|4|5)\\s+")
        if (!m.containsMatchIn(t)) return t
        val parts = t.split(
            Regex("(?i)(?:number|item)\\s+(?:one|two|three|four|five|1|2|3|4|5)\\s+")
        ).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return t
        return parts.mapIndexed { i, p ->
            "${i + 1}. ${p.trim().trimEnd('.', ',')}"
        }.joinToString("\n")
    }

    private fun applyPunctuation(t: String, style: TextPostProcessor.Style): String {
        var s = t
        if (s.lastOrNull()?.isLetterOrDigit() == true) {
            s += when (style) {
                TextPostProcessor.Style.FORMAL -> "."
                TextPostProcessor.Style.EXCITED -> "!"
                else -> if (s.length > 40) "." else ""
            }
        }
        val qStarts = listOf(
            "who ", "what ", "where ", "when ", "why ", "how ",
            "is ", "are ", "can ", "do "
        )
        if (qStarts.any { s.startsWith(it, ignoreCase = true) } && !s.endsWith("?")) {
            s = s.trimEnd('.', '!') + "?"
        }
        return s
    }

    private fun applyCapitalization(t: String): String {
        if (t.isEmpty()) return t
        val sb = StringBuilder(t)
        sb[0] = sb[0].uppercaseChar()
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
}
