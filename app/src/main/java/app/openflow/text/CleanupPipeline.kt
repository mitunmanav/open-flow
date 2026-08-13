package app.openflow.text

/**
 * Local Auto Cleanup (Wispr levels, no cloud AI).
 *
 * | Level  | Stages |
 * |--------|--------|
 * | None   | identity |
 * | Light  | normalize → fillers → reps → VoiceCommands ([PhraseMap]) → lightGrammar |
 * | Medium | Light → false starts → CourseCorrector → lists → lightClarity |
 * | High   | Medium → hedge/wordiness strip (rules only) |
 *
 * Writing style is applied *after* levels via [StyleApplicator] / [SentenceFormat].
 * Empty in → empty out. Non-empty content must not vanish (except explicit clear).
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
        t = keepContent(original, t)
        t = StyleApplicator.apply(t, style, custom)
        t = keepContent(original, t)

        return CleanupResult(
            raw = original.trim().ifEmpty { original },
            clean = t,
            corrections = corrections,
            level = level
        )
    }

    /** Explicit wipe commands may empty. Fillers-only may empty. Real words stay. */
    private fun keepContent(original: String, clean: String): String {
        if (clean.isNotBlank()) return clean
        if (isExplicitWipe(original)) return clean
        if (!hasContentWords(original)) return clean
        val recovered = stripFillers(collapseRepetitions(normalize(original.trim())))
        return recovered.ifBlank { original.trim() }
    }

    private val wipePhrase = Regex(
        """(?i)\b(?:clear\s+all|clear\s+everything|delete\s+all)\b"""
    )

    private fun isExplicitWipe(raw: String): Boolean = wipePhrase.containsMatchIn(raw)

    private fun hasContentWords(raw: String): Boolean =
        raw.split(ws).any { tok ->
            val n = tok.lowercase().trim(',', '.', '!', '?', ';', ':')
            n.isNotEmpty() && n !in fillerSet
        }

    // ---- Light stages ----

    private val ws = Regex("\\s+")
    private val horizWs = Regex("[ \\t]+")
    private val manyNewlines = Regex("\n{3,}")
    private val doubleComma = Regex("""\s*,\s*,+""")
    private val spaceComma = Regex("""\s+,""")
    private val likeCommaFiller = Regex("""(?i),\s*like\s*,""")
    private val likeBeforeFiller = Regex("""(?i)(?<=\s)like(?=\s+(?:um|uh|you know)\b)""")

    internal fun normalize(t: String): String =
        t.replace(ws, " ").trim()

    /** Collapse horizontal space; keep newlines for lists / spoken line breaks. */
    internal fun normalizeKeepNewlines(t: String): String =
        t.lines()
            .joinToString("\n") { it.replace(horizWs, " ").trim() }
            .replace(manyNewlines, "\n\n")
            .trim()

    private val fillers = listOf(
        "um", "uh", "erm", "ah", "uhm", "hmm", "mhm",
        "you know", "sort of", "kind of"
    )
    private val fillerSet = fillers.toHashSet()

    private val fillerRegexes: List<Regex> = fillers
        .sortedByDescending { it.length }
        .map { f -> Regex("\\b${Regex.escape(f)}\\b[,\\s]*", RegexOption.IGNORE_CASE) }

    private val repeatPhrase = Regex("""(?i)\b(\w+(?:\s+\w+){0,2})\s+\1\b""")
    private val loneI = Regex("""\bi\b""")
    private val spaceBeforePunct = Regex("""\s+([,.!?;:])""")
    private val punctNeedSpace = Regex("""([,.!?;:])([A-Za-z])""")
    private val falseStartGoing = Regex("""(?i)\bI\s+was\s+going\s+to\b[^.!?\n]*?[—–-]\s*""")
    private val falseStartStarted = Regex("""(?i)\bI\s+started\s+to\b[^.!?\n]*?[—–-]\s*""")
    private val clarityOpenerComma = Regex("""(?i)^(well|so|okay|ok|right|anyway)\s*,\s*""")
    private val clarityOpenerSpace = Regex("""(?i)^(well|so|okay|ok|right|anyway)\s+""")
    private val spaceBeforeDot = Regex("""\s+\.""")
    private val dottedListSplit = Regex("""\s+(?=\d+\.\s+)""")
    private val dottedListItem = Regex("""^\d+\.\s+\S.*""")
    private val spokenDigitSplit = Regex("""\s+(?=\d{1,2}\s+[A-Za-z])""")
    private val spokenDigitItem = Regex("""^\d{1,2}\s+\S.*""")
    private val spokenDigitBody = Regex("""^(\d{1,2})\s+(.+)$""")
    private val spokenNumberItem = Regex("(?i)(?:number|item)\\s+(one|two|three|four|five|1|2|3|4|5)\\s+")
    private val spokenNumberSplit = Regex("(?i)(?:number|item)\\s+(?:one|two|three|four|five|1|2|3|4|5)\\s+")
    private val justHedge = Regex("""(?i)\bjust\s+(?=(?:want|need|think|go|do|say|try)\b)""")
    private val hedgePhrases: List<Pair<Regex, String>> = listOf(
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
    private val hedgeWords = listOf(
        "basically", "literally", "actually", "really", "quite", "honestly", "obviously"
    )
    private val hedgeWordRegexes: List<Regex> = hedgeWords.map { h ->
        Regex("\\b${Regex.escape(h)}\\b[,\\s]*", RegexOption.IGNORE_CASE)
    }

    internal fun stripFillers(t: String): String {
        var out = t
        fillerRegexes.forEach { re ->
            out = re.replace(out, " ")
        }
        out = doubleComma.replace(out, ",")
        // ", like," filler (not "I like pizza")
        out = likeCommaFiller.replace(out, " like ")
        out = likeBeforeFiller.replace(out, " ")
        out = doubleComma.replace(out, ",")
        out = spaceComma.replace(out, ",")
        return normalize(out)
    }

    internal fun collapseRepetitions(t: String): String {
        val words = t.split(ws).filter { it.isNotBlank() }
        if (words.size < 2) return t
        val out = ArrayList<String>(words.size)
        for (w in words) {
            val prev = out.lastOrNull()
            if (prev != null && prev.equals(w, ignoreCase = true)) continue
            out.add(w)
        }
        var s = out.joinToString(" ")
        s = repeatPhrase.replace(s, "$1")
        return normalize(s)
    }

    /** Light grammar: lone i→I, punct spacing. Not style, not hedges. */
    internal fun lightGrammar(t: String): String {
        var s = t
        s = loneI.replace(s, "I")
        s = spaceBeforePunct.replace(s, "$1")
        s = punctNeedSpace.replace(s, "$1 $2")
        return normalizeKeepNewlines(s)
    }

    // ---- Medium stages ----

    internal fun stripFalseStarts(t: String): String {
        var s = t
        // Abandoned clause before dash rethink: "I was going to call — never mind"
        s = falseStartGoing.replace(s, "")
        s = falseStartStarted.replace(s, "")
        return normalizeKeepNewlines(s)
    }

    /** Drop leading empty discourse openers (first line). */
    internal fun lightClarity(t: String): String {
        var s = t
        s = clarityOpenerComma.replace(s, "")
        s = clarityOpenerSpace.replace(s, "")
        return normalizeKeepNewlines(s)
    }

    internal fun applyListHints(t: String): String {
        splitDottedNumbered(t)?.let { return it }
        splitSpokenDigitList(t)?.let { return it }
        if (!spokenNumberItem.containsMatchIn(t)) return t
        val parts = t.split(spokenNumberSplit).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return t
        return parts.mapIndexed { i, p ->
            "${i + 1}. ${p.trim().trimEnd('.', ',')}"
        }.joinToString("\n")
    }

    /** "1. Apples 2. Bananas 3. Oranges" → multiline. */
    internal fun splitDottedNumbered(t: String): String? {
        val trimmed = t.trim()
        val parts = trimmed.split(dottedListSplit).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        if (!parts.all { it.matches(dottedListItem) }) return null
        return parts.joinToString("\n") { p ->
            p.trimEnd('.', ',', ';')
        }
    }

    /** "1 apples 2 bananas 3 oranges" → multiline list. */
    private fun splitSpokenDigitList(t: String): String? {
        val trimmed = t.trim()
        if (trimmed.isEmpty() || !trimmed.first().isDigit()) return null
        val parts = trimmed.split(spokenDigitSplit).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        if (!parts.all { it.matches(spokenDigitItem) }) return null
        return parts.map { p ->
            val m = spokenDigitBody.find(p) ?: return null
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
        for ((re, rep) in hedgePhrases) {
            s = re.replace(s, rep)
        }
        hedgeWordRegexes.forEach { re ->
            s = re.replace(s, " ")
        }
        s = justHedge.replace(s, "")
        s = doubleComma.replace(s, ",")
        s = spaceComma.replace(s, ",")
        s = spaceBeforeDot.replace(s, ".")
        return normalize(s)
    }

    /** @deprecated Use [stripHedges]. Kept for any external callers. */
    internal fun polishBrevity(t: String): String = stripHedges(t)
}
