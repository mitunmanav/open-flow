package app.openflow.text

/**
 * Runtime invariant gate (spec: docs/specs/offline-smart.md, invariants I1-I4).
 *
 * I1 No invention — every output word is a substring of an input word
 *    (case-insensitive), a deterministic transform (ITN digit/symbol rendering,
 *    StyleApplicator expansion table), or pure punctuation.
 * I2 Terminal punctuation — input ending in `?`/`!` still ends with `?`/`!`
 *    unless the last word was a stripped filler/tag. A trailing `.` may be
 *    dropped by TrailingPeriodPolicy (casual/messaging).
 * I3 Totality / I4 Idempotence are enforced by the wiring in [CleanupPipeline]
 *    (never throws) and by seeded property tests; this object stays cheap so the
 *    runtime gate only pays for the word-origin scan.
 *
 * A violation never crashes — callers fall back to the light pass or raw text.
 */
object InvariantGate {

    /** AtomicTokens protect/restore sentinel chars count as word characters. */
    private const val SENTINELS = "\uE000\uE001"

    /**
     * Symbols a deterministic render may emit: ITN money/time/date/phone shapes,
     * voice-command punctuation table, list bullets.
     */
    private val renderSymbols = (
        "0123456789" +
            "$€£₹¥%°©®™" +
            ":/\\-.,@+=()#&*~<>[]\"'" +
            "•" + SENTINELS
        ).toHashSet()

    /**
     * Whole words emitted by deterministic transforms that are not substrings of
     * anything the user said:
     * - StyleApplicator informal expansions ("gonna" -> "going to")
     * - CleanupPipeline hedge rewrites ("due to the fact that" -> "because")
     * - ITN time-of-day markers
     */
    private val transformWords = setOf(
        "am", "pm", "because", "now",
        "going", "to", "want", "got", "kind", "of", "sort",
        "do", "not", "know", "is", "cannot", "will", "does",
        "are", "was", "were", "it", "that", "there",
        "we", "they", "you", "have", "would",
    )

    /** ITN ordinal render: 1st / 2nd / 3rd / 4th … */
    private val ordinalRender = Regex("""\d+(st|nd|rd|th)""")

    /** Alphanumeric runs inside a token ("7bucks" -> ["7", "bucks"]). */
    private val alnumRuns = Regex("[A-Za-z0-9]+")

    /**
     * Deterministic glues are fine when every piece came from the user's
     * words: ITN money ("7bucks"), VoiceCommands quotes (said"hello"),
     * ItnElectronic renders (john@gmail.com, example.com/docs).
     */
    private fun piecesFromOrigin(core: String, originCores: List<String>): Boolean =
        alnumRuns.findAll(core)
            .all { run -> originCores.any { it.contains(run.value) } }

    data class Verdict(val ok: Boolean, val invented: List<String>)

    /**
     * I1 word-origin check. Punctuation-only tokens always pass.
     */
    fun check(original: String, clean: String): Verdict {
        if (clean.isBlank()) return Verdict(true, emptyList())
        val originCores = cores(original).map { it.lowercase() }
        val invented = mutableListOf<String>()
        for (core in cores(clean)) {
            val low = core.lowercase()
            if (low.isEmpty()) continue // punctuation-only token
            if (originCores.any { it.contains(low) }) continue
            if (core.all { it in renderSymbols }) continue
            if (ordinalRender.matches(low)) continue
            if (piecesFromOrigin(low, originCores)) continue
            if (low in transformWords) continue
            invented.add(core)
        }
        return Verdict(invented.isEmpty(), invented)
    }

    fun ok(original: String, clean: String): Boolean =
        check(original, clean).ok && terminalPunctOk(original, clean)

    /**
     * I2: `?`/`!` on the input must still terminate the output, unless the
     * last original word was a filler/tag that cleanup dropped. Periods are
     * allowed to disappear (style).
     */
    fun terminalPunctOk(original: String, clean: String): Boolean {
        val o = original.trimEnd()
        val c = clean.trimEnd()
        if (o.isEmpty() || c.isEmpty()) return true
        val term = o.last()
        if (term != '?' && term != '!') return true
        if (c.last() == '?' || c.last() == '!') return true
        val lastWord = o.trimEnd('.', '!', '?').trim()
            .split(whitespace)
            .lastOrNull()
            ?.lowercase()
            ?.trim(',', ';', ':')
            .orEmpty()
        return lastWord in fillerishLast
    }

    private val fillerishLast = setOf(
        "um", "uh", "er", "ah", "right", "yeah", "okay", "ok", "hmm",
    )

    /** Word cores: whitespace-split, surrounding punctuation stripped, sentinels kept. */
    private fun cores(text: String): List<String> =
        text.split(whitespace)
            .filter { it.isNotEmpty() }
            .map { tok ->
                var s = 0
                var e = tok.length
                while (s < e && !wordChar(tok[s])) s++
                while (e > s && !wordChar(tok[e - 1])) e--
                tok.substring(s, e)
            }
            .filter { it.isNotEmpty() }

    private fun wordChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '\'' || c in SENTINELS

    private val whitespace = Regex("\\s+")
}
