package app.openflow.text

/**
 * Local Auto Cleanup (Wispr levels, no cloud AI).
 *
 * | Level  | Stages |
 * |--------|--------|
 * | None   | identity |
 * | Light  | normalize → fillers → reps → VoiceCommands → lightGrammar |
 * | Medium | Light → false starts → CourseCorrector → lists → lightClarity |
 * | High   | Medium → hedge/wordiness strip (rules only) |
 *
 * Writing style is applied *after* levels via [StyleApplicator] — never by level.
 */
object CleanupPipeline {

    fun run(
        raw: String,
        level: CleanupLevel = CleanupLevel.NORMAL,
        style: WritingStyle = WritingStyle.CASUAL,
        custom: CustomStyleConfig = CustomStyleConfig()
    ): CleanupResult {
        val original = raw
        if (original.isBlank()) {
            return CleanupResult(raw = "", clean = "", level = level)
        }
        if (level == CleanupLevel.RAW) {
            return CleanupResult(raw = original, clean = original, level = level)
        }

        var t = original.trim()
        val corrections = mutableListOf<Correction>()

        // Light (all non-RAW)
        t = normalize(t)
        t = stripFillers(t)
        t = collapseRepetitions(t)
        t = VoiceCommands.apply(t)
        t = lightGrammar(t)

        // Medium+
        if (level == CleanupLevel.NORMAL || level == CleanupLevel.HIGH) {
            t = stripFalseStarts(t)
            val analyzed = CourseCorrector.analyze(t)
            corrections += analyzed.corrections
            t = analyzed.text
            t = applyListHints(t)
            t = lightClarity(t)
        }

        // High only
        if (level == CleanupLevel.HIGH) {
            t = stripHedges(t)
        }

        t = normalizeKeepNewlines(t)
        t = StyleApplicator.apply(t, style, custom)

        return CleanupResult(
            raw = original.trim().ifEmpty { original },
            clean = t,
            corrections = corrections,
            level = level
        )
    }

    // ---- Light stages ----

    internal fun normalize(t: String): String =
        t.replace(Regex("\\s+"), " ").trim()

    /** Collapse horizontal space; keep newlines for lists / spoken line breaks. */
    internal fun normalizeKeepNewlines(t: String): String =
        t.lines()
            .joinToString("\n") { it.replace(Regex("[ \\t]+"), " ").trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

    private val fillers = listOf(
        "um", "uh", "erm", "ah", "uhm", "hmm", "mm", "mhm",
        "you know", "sort of", "kind of"
    )

    internal fun stripFillers(t: String): String {
        var out = t
        fillers.sortedByDescending { it.length }.forEach { f ->
            out = out.replace(
                Regex("\\b${Regex.escape(f)}\\b[,\\s]*", RegexOption.IGNORE_CASE),
                " "
            )
        }
        out = out.replace(Regex("""\s*,\s*,+"""), ",")
        // ", like," filler (not "I like pizza")
        out = out.replace(Regex("""(?i),\s*like\s*,"""), " like ")
        out = out.replace(
            Regex("""(?i)(?<=\s)like(?=\s+(?:um|uh|you know)\b)"""),
            " "
        )
        out = out.replace(Regex("""\s*,\s*,+"""), ",")
        out = out.replace(Regex("""\s+,"""), ",")
        return normalize(out)
    }

    internal fun collapseRepetitions(t: String): String {
        val words = t.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 2) return t
        val out = ArrayList<String>(words.size)
        for (w in words) {
            val prev = out.lastOrNull()
            if (prev != null && prev.equals(w, ignoreCase = true)) continue
            out.add(w)
        }
        var s = out.joinToString(" ")
        s = s.replace(
            Regex("""(?i)\b(\w+(?:\s+\w+){0,2})\s+\1\b"""),
            "$1"
        )
        return normalize(s)
    }

    /** Light grammar: lone i→I, punct spacing. Not style, not hedges. */
    internal fun lightGrammar(t: String): String {
        var s = t
        s = s.replace(Regex("""\bi\b"""), "I")
        s = s.replace(Regex("""\s+([,.!?;:])"""), "$1")
        s = s.replace(Regex("""([,.!?;:])([A-Za-z])"""), "$1 $2")
        return normalizeKeepNewlines(s)
    }

    // ---- Medium stages ----

    internal fun stripFalseStarts(t: String): String {
        var s = t
        // Abandoned clause before dash rethink: "I was going to call — never mind"
        s = s.replace(Regex("""(?i)\bI\s+was\s+going\s+to\b[^.!?\n]*?[—–-]\s*"""), "")
        s = s.replace(Regex("""(?i)\bI\s+started\s+to\b[^.!?\n]*?[—–-]\s*"""), "")
        return normalizeKeepNewlines(s)
    }

    /** Drop leading empty discourse openers (first line). */
    internal fun lightClarity(t: String): String {
        var s = t
        s = s.replace(Regex("""(?i)^(well|so|okay|ok|right|anyway)\s*,\s*"""), "")
        s = s.replace(Regex("""(?i)^(well|so|okay|ok|right|anyway)\s+"""), "")
        return normalizeKeepNewlines(s)
    }

    internal fun applyListHints(t: String): String {
        splitDottedNumbered(t)?.let { return it }
        splitSpokenDigitList(t)?.let { return it }
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

    /** "1. Apples 2. Bananas 3. Oranges" → multiline. */
    internal fun splitDottedNumbered(t: String): String? {
        val trimmed = t.trim()
        val parts = trimmed.split(Regex("""\s+(?=\d+\.\s+)""")).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        if (!parts.all { it.matches(Regex("""^\d+\.\s+\S.*""")) }) return null
        return parts.joinToString("\n") { p ->
            p.trimEnd('.', ',', ';')
        }
    }

    /** "1 apples 2 bananas 3 oranges" → multiline list. */
    private fun splitSpokenDigitList(t: String): String? {
        val trimmed = t.trim()
        if (trimmed.isEmpty() || !trimmed.first().isDigit()) return null
        val parts = trimmed.split(Regex("""\s+(?=\d{1,2}\s+[A-Za-z])""")).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        if (!parts.all { it.matches(Regex("""^\d{1,2}\s+\S.*""")) }) return null
        return parts.map { p ->
            val m = Regex("""^(\d{1,2})\s+(.+)$""").find(p) ?: return null
            val body = m.groupValues[2].trim().trimEnd('.', ',', ';')
            "${m.groupValues[1]}. $body"
        }.joinToString("\n")
    }

    // ---- High stages ----

    /**
     * Short hedge / wordiness rules only.
     * Not Formal style. Not LLM rewrite.
     */
    internal fun stripHedges(t: String): String {
        var s = t
        val phrases = listOf(
            Regex("""(?i)\bdue\s+to\s+the\s+fact\s+that\b""") to "because",
            Regex("""(?i)\bin\s+order\s+to\b""") to "to",
            Regex("""(?i)\bat\s+this\s+point\s+in\s+time\b""") to "now",
            Regex("""(?i)\bfor\s+all\s+intents\s+and\s+purposes,?\s*""") to "",
            Regex("""(?i)\bto\s+be\s+honest,?\s*""") to "",
            Regex("""(?i)\bneedless\s+to\s+say,?\s*""") to "",
            Regex("""(?i)\bI\s+would\s+say\s+(?:that\s+)?""") to "",
            Regex("""(?i)\bI\s+think\s+that\b""") to "",
            Regex("""(?i)\bI\s+feel\s+like\b""") to "",
            Regex("""(?i)\bit\s+seems\s+(?:like|that)\b""") to "",
            Regex("""(?i)\bI\s+guess\s+(?:that\s+)?""") to "",
            Regex("""(?i)\band\s+so\s+on\b""") to "",
            Regex("""(?i)\band\s+stuff\b""") to "",
            Regex("""(?i)\bor\s+whatever\b""") to "",
            Regex("""(?i)\bpretty\s+much\b""") to "",
            Regex("""(?i)\ba\s+little\s+bit\b""") to "a bit",
        )
        for ((re, rep) in phrases) {
            s = re.replace(s, rep)
        }
        val hedges = listOf(
            "basically", "literally", "actually", "really", "quite", "honestly", "obviously"
        )
        hedges.forEach { h ->
            s = s.replace(
                Regex("\\b${Regex.escape(h)}\\b[,\\s]*", RegexOption.IGNORE_CASE),
                " "
            )
        }
        s = s.replace(Regex("""(?i)\bjust\s+(?=(?:want|need|think|go|do|say|try)\b)"""), "")
        s = s.replace(Regex("""\s*,\s*,+"""), ",")
        s = s.replace(Regex("""\s+,"""), ",")
        s = s.replace(Regex("""\s+\."""), ".")
        return normalize(s)
    }

    /** @deprecated Use [stripHedges]. Kept for any external callers. */
    internal fun polishBrevity(t: String): String = stripHedges(t)
}
