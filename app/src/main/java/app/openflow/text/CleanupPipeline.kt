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
        custom: CustomStyleConfig = CustomStyleConfig(),
        messaging: Boolean = false,
        spokenEmoji: Boolean = false,
    ): CleanupResult {
        // I3 totality: user text can be anything — the pipeline must never throw.
        return try {
            runGated(raw, level, style, custom, messaging, spokenEmoji)
        } catch (t: Throwable) {
            CleanupResult(raw = raw.trim(), clean = raw.trim(), level = level)
        }
    }

    private fun runGated(
        raw: String,
        level: CleanupLevel,
        style: WritingStyle,
        custom: CustomStyleConfig,
        messaging: Boolean,
        spokenEmoji: Boolean,
    ): CleanupResult {
        val original = raw
        if (original.isBlank()) {
            return CleanupResult(raw = "", clean = "", level = level)
        }
        if (level == CleanupLevel.RAW) {
            return CleanupResult(raw = original, clean = original, level = level)
        }

        val result = converge(original, level, style, custom, messaging, spokenEmoji)

        // I4 stability + I1 no invention: an unstable or inventing run falls
        // back to the converged light pass (same gates); if even that fails,
        // hand back honest raw text. Never crash, never invent.
        if (result.second && InvariantGate.ok(original, result.first.clean)) {
            return result.first
        }
        if (level != CleanupLevel.LIGHT) {
            val light = converge(original, CleanupLevel.LIGHT, style, custom, messaging, spokenEmoji)
            if (light.second && InvariantGate.ok(original, light.first.clean)) {
                return light.first
            }
        }
        return CleanupResult(
            raw = original.trim(),
            clean = normalizeKeepNewlines(original),
            corrections = emptyList(),
            level = level
        )
    }

    /** Max stage sweeps to reach a fixed point (I4 idempotence). */
    private const val MAX_PASSES = 3

    /**
     * Runs [stages] repeatedly until the output stops changing, so
     * clean(clean(x)) == clean(x) holds by construction. Second element is
     * false when [MAX_PASSES] sweeps still mutate the text.
     */
    private fun converge(
        input: String,
        level: CleanupLevel,
        style: WritingStyle,
        custom: CustomStyleConfig,
        messaging: Boolean,
        spokenEmoji: Boolean,
    ): Pair<CleanupResult, Boolean> {
        var cur = input
        var res = stages(cur, level, style, custom, messaging, spokenEmoji)
        val first = res
        var passes = 1
        while (res.clean != cur && passes < MAX_PASSES) {
            cur = res.clean
            res = stages(cur, level, style, custom, messaging, spokenEmoji)
            passes++
        }
        val stable = res.clean == cur
        // Raw + corrections always come from the FIRST sweep over the user's
        // text; later sweeps only refine the clean string.
        return first.copy(clean = res.clean) to stable
    }

    private fun stages(
        original: String,
        level: CleanupLevel,
        style: WritingStyle,
        custom: CustomStyleConfig,
        messaging: Boolean,
        spokenEmoji: Boolean,
    ): CleanupResult {
        // Newline-preserving entry: sweeps 2+ must not undo inserted line breaks.
        var t = normalizeKeepNewlines(original.trim())
        val corrections = mutableListOf<Correction>()
        val protected0 = AtomicTokens.protect(t)
        var atomicSpans = protected0.spans
        t = protected0.text
        // Spoken addresses before VoiceCommands so "dot"/"slash" inside them
        // survive the punctuation table; then wrap the symbolic forms.
        t = ItnElectronic.apply(t)
        val protectedE = AtomicTokens.protect(t)
        atomicSpans += protectedE.spans
        t = protectedE.text
        t = stripFillers(t)
        t = StutterCollapse.apply(t)
        t = repeatPhrase.replace(t, "$1")
        t = VoiceCommands.apply(t)
        t = SpokenEmoji.apply(t, spokenEmoji)
        t = RunOnSplitPolicy.apply(t)
        t = lightGrammar(t)

        // Medium+
        if (level == CleanupLevel.NORMAL || level == CleanupLevel.HIGH) {
            val protected1 = AtomicTokens.protect(t)
            atomicSpans += protected1.spans
            t = Itn.apply(protected1.text)
            t = stripFalseStarts(t)
            val hadLeadNl = t.startsWith("\n")
            val analyzed = CourseCorrector.analyze(t)
            corrections += analyzed.corrections
            t = analyzed.text
            if (hadLeadNl && !t.startsWith("\n")) t = "\n" + t
            t = applyListHints(t)
            t = SerialCommaPolicy.apply(t)
            t = lightClarity(t)
            // Openers stripped above can expose an interjection at the start.
            t = interjection.replace(t) { m ->
                m.groupValues[1].replaceFirstChar { it.uppercase() } + ", "
            }
        }

        // High only
        if (level == CleanupLevel.HIGH) {
            t = stripHedges(t)
        }

        t = normalizeKeepNewlines(t)
        t = keepContent(original, t)
        t = StyleApplicator.apply(t, style, custom)
        t = TrailingPeriodPolicy.apply(t, style, messaging)

        // Restore protected spans last so punctuation/caps stages never touch them.
        t = AtomicTokens.restore(t, atomicSpans)
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
        return when (KeepContentPolicy.decide(clean, isExplicitWipe(original), hasContentWords(original))) {
            KeepContentPolicy.Kind.KEEP_CLEAN -> clean
            KeepContentPolicy.Kind.ALLOW_EMPTY -> clean
            KeepContentPolicy.Kind.RECOVER -> {
                val recovered = stripFillers(collapseRepetitions(normalize(original.trim())))
                recovered.ifBlank { original.trim() }
            }
        }
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
    internal fun normalizeKeepNewlines(t: String): String {
        val lead = if (t.startsWith("\n")) "\n" else ""
        return lead + t.lines()
            .joinToString("\n") { it.replace(horizWs, " ").trim() }
            .replace(manyNewlines, "\n\n")
            .trimEnd()
    }

    private val fillers = listOf(
        "um", "uh", "erm", "ah", "uhm", "hmm", "mhm",
        "you know", "sort of", "kind of",
        "i mean", "uh huh", "uh-huh", "mm hmm", "mm-hmm"
    )
    private val fillerSet = fillers.toHashSet()

    private val fillerRegexes: List<Regex> = fillers
        .sortedByDescending { it.length }
        .map { f -> Regex("\\b${Regex.escape(f)}\\b[,\\s]*", RegexOption.IGNORE_CASE) }

    /** Stretched sounds from STT: umm, uhhh, hmmm, ahhh, erm, errr. \b keeps real words safe. */
    private val stretchedFillerRegexes: List<Regex> = listOf(
        "\\bu+m+\\b",
        "\\bu+h+m?\\b",
        "\\bah+\\b",
        "\\bh+m+\\b",
        "\\bermm*\\b",
        "\\ber+r+m*\\b",
        "\\bm+h+m*\\b",
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private val repeatPhrase = Regex("""(?i)\b(\w+(?:\s+\w+){1,2})\s+\1\b""")
    private val loneI = Regex("""\bi\b""")
    private val spaceBeforePunct = Regex("""\s+([,.!?;:])""")
    private val punctNeedSpace = Regex("""([,.!?;:])([A-Za-z])""")
    private val falseStartGoing = Regex("""(?i)\bI\s+was\s+going\s+to\b[^.!?\n]*?[—–-]\s*""")
    private val falseStartStarted = Regex("""(?i)\bI\s+started\s+to\b[^.!?\n]*?[—–-]\s*""")
    private val clarityOpenerComma = Regex("""(?i)^(well|so|okay|ok|right|anyway)\s*,\s*""")
    // "well"/"anyway" kept as openers: "well done everyone" is a compliment,
    // "anyway, we left" is a real transition — not filler.
    private val clarityOpenerSpace = Regex("""(?i)^(so|okay|ok|anyway)\s+""")
    /** Interjections that take a comma before the rest of the sentence. */
    private val interjection = Regex(
        """(?i)^(yes|yeah|nope|sure|correct|exactly|anyway|also)(\s+)(?=[a-z])"""
    )
    /** Discourse "like" opening a clause ("like we need to...") is filler. */
    private val clauseLike = Regex(
        """(?i)^like\s+(?=(?:we|i|you|they|he|she|it)\b)"""
    )
    private val spaceBeforeDot = Regex("""\s+\.""")
    private val dottedListSplit = Regex("""\s+(?=\d+\.\s+)""")
    private val dottedListItem = Regex("""^\d+\.\s+\S.*""")
    private val spokenDigitSplit = Regex("""\s+(?=\d{1,2}\s+[A-Za-z])""")
    private val spokenDigitItem = Regex("""^\d{1,2}\s+\S.*""")
    private val spokenDigitBody = Regex("""^(\d{1,2})\s+(.+)$""")
    private val sequenceWordLead = Regex(
        """(?i)(?:first|second|third|fourth|fifth)\s+"""
    )
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

    /**
     * Content uses that must survive filler stripping: correction triggers
     * feed CourseCorrector later; emphasis phrases are real content.
     */
    internal val fillerKeepPhrases = listOf(
        Regex("""(?i)\bi\s+mean\s+(?=[a-z])"""),
        Regex("""(?i)\bi\s+meant\s+(?=[a-z])"""),
        Regex("""(?i)\byou\s+know\s+(?=[a-z])"""),
        Regex("""(?i)\bwhat\s+i\s+mean\b"""),
        Regex("""(?i)\bwell\s+done\b"""),
    )

    /** Line-local so inserted newlines (voice commands) survive re-passes. */
    internal fun stripFillers(t: String): String {
        if (!t.contains('\n')) return stripFillersSingle(t)
        return t.split("\n").joinToString("\n") { line -> stripFillersSingle(line) }
    }

    private fun stripFillersSingle(t: String): String {
        // Protect content phrases from filler removal.
        var work = t
        val guards = ArrayList<Pair<String, String>>()
        fillerKeepPhrases.forEachIndexed { idx, re ->
            re.findAll(work).toList().reversed().forEach { m ->
                val token = "\uE100${idx}x${m.range.first}\uE101"
                guards.add(token to m.value)
                work = work.substring(0, m.range.first) + token +
                    work.substring(m.range.last + 1)
            }
        }
        var out = work
        fillerRegexes.forEach { re ->
            out = re.replace(out, " ")
        }
        stretchedFillerRegexes.forEach { re ->
            out = re.replace(out, " ")
        }
        out = doubleComma.replace(out, ",")
        // ", like," filler (not "I like pizza")
        out = likeCommaFiller.replace(out, " like ")
        out = likeBeforeFiller.replace(out, " ")
        out = doubleComma.replace(out, ",")
        out = spaceComma.replace(out, ",")
        guards.forEach { (token, original) -> out = out.replace(token, original) }
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

    /** Light grammar: lone i→I, punct spacing, interjection commas. */
    internal fun lightGrammar(t: String): String {
        var s = t
        s = clauseLike.replace(s, "")
        s = interjection.replace(s) { m ->
            m.groupValues[1].replaceFirstChar { it.uppercase() } + ", "
        }
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
        var prev: String
        do {
            prev = s
            s = clarityOpenerComma.replace(s, "")
            s = clarityOpenerSpace.replace(s, "")
        } while (s != prev)
        return normalizeKeepNewlines(s)
    }

    internal fun applyListHints(t: String): String {
        inlineEnumeration(t)?.let { return it }
        spokenPairList(t)?.let { return it }
        splitDottedNumbered(t)?.let { return it }
        splitSpokenDigitList(t)?.let { return it }
        splitSequenceWordList(t)?.let { return it }
        if (!spokenNumberItem.containsMatchIn(t)) return t
        val parts = t.split(spokenNumberSplit).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return t
        return parts.mapIndexed { i, p ->
            "${i + 1}. ${p.trim().trimEnd('.', ',')}"
        }.joinToString("\n")
    }

    private val numWord1to12 = setOf(
        "one", "two", "three", "four", "five", "six",
        "seven", "eight", "nine", "ten", "eleven", "twelve",
    )
    /** "steps are one install two configure three run" → "… 1. install 2. …" */
    private val enumCue = Regex(
        """(?i)\b(steps|agenda|options|list|plan)\s+(?:are|is)\s+"""
    )
    private val numWordToken = Regex("""(?i)^(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)$""")

    /** Inline numbered run after an explicit cue: replace number words with "N.". */
    private fun inlineEnumeration(t: String): String? {
        val m = enumCue.find(t) ?: return null
        val rest = t.substring(m.range.last + 1)
        val tokens = rest.split(" ")
        val out = ArrayList<String>(tokens.size)
        var n = 0
        for (tok in tokens) {
            if (numWordToken.matches(tok.trim(',')) && n < 12) {
                n++
                out.add("$n.")
            } else {
                out.add(tok)
            }
        }
        if (n < 2) return null
        return t.substring(0, m.range.last + 1) + out.joinToString(" ")
    }

    /** Whole-utterance spoken pairs: "one coffee two tea" / "one, apples two, bananas". */
    private fun isListNumWord(tok: String): Boolean =
        numWordToken.matches(tok.trim(',').trimEnd('.'))

    private fun spokenPairList(t: String): String? {
        if (t.contains('\n')) return null
        val trimmed = t.trim().trimEnd('.', '!', '?')
        val tokens = trimmed.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        var i = 0
        if (tokens[0].equals("the", ignoreCase = true)) i = 1
        if (i >= tokens.size || !isListNumWord(tokens[i])) return null
        val items = ArrayList<String>()
        while (i < tokens.size) {
            if (!isListNumWord(tokens[i])) return null
            i++
            val noun = StringBuilder()
            while (i < tokens.size && !isListNumWord(tokens[i])) {
                noun.append(tokens[i]).append(' ')
                i++
            }
            val body = noun.toString().trim().trimEnd(',', ';')
            if (body.isEmpty()) return null
            items.add(body)
        }
        if (items.size < 2) return null
        return items.mapIndexed { idx, p -> "${idx + 1}. $p" }.joinToString("\n")
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

    /** "first … second …" → multiline list. Intro text before the first ordinal is dropped. */
    private fun splitSequenceWordList(t: String): String? {
        if (t.contains('\n')) return null  // layout commands already structured it
        val trimmed = t.trim()
        if (sequenceWordLead.findAll(trimmed).count() < 2) return null
        val chunks = trimmed.split(Regex("(?i)\\s+(?=first|second|third|fourth|fifth)\\b"))
        val ordinal = Regex("(?i)^(first|second|third|fourth|fifth)\\s+")
        val bodies = ArrayList<String>()
        chunks.forEachIndexed { i, chunk ->
            val c = chunk.trim()
            val hasOrd = ordinal.containsMatchIn(c)
            if (!hasOrd && i == 0) return@forEachIndexed
            val body = ordinal.replace(c, "").trim().trimEnd('.', ',')
            if (body.isNotEmpty()) bodies += body
        }
        if (bodies.size < 2) return null
        return bodies.mapIndexed { i, p -> "${i + 1}. $p" }.joinToString("\n")
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
            s = normalize(s)
        }
        hedgeWordRegexes.forEach { re ->
            s = re.replace(s, " ")
            s = normalize(s)
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
