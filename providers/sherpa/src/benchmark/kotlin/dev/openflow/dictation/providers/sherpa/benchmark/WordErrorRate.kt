package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * What a recognition error is counted in.
 *
 * This is not a detail, and getting it wrong makes the WER column meaningless
 * rather than merely inaccurate — which is the one thing ticket 40 says the
 * harness must not do.
 *
 * English separates words with spaces, so a word-level score is the standard and
 * a usable one. **Chinese does not**: SenseVoice and the bilingual zipformers
 * emit `你好世界` as one unbroken run with no spaces anywhere, so splitting on
 * whitespace yields a single token and every reference scores as either 0% or
 * 100% regardless of what was actually said. Chinese and Japanese dictation is
 * therefore scored per **character**, which is the convention those languages'
 * ASR literature uses.
 *
 * The unit is a property of the **eval set**, not of the model, and is declared in
 * the run manifest rather than sniffed from the audio. Two of the eight
 * candidates are bilingual, so the same model legitimately gets scored in
 * different units against different sets, and a harness that inferred the unit
 * from the model would quietly compare two different quantities in one table.
 */
enum class ScoreUnit {
    /** Split on whitespace after normalization. WER. */
    WORD,

    /** One token per character after normalization. CER. */
    CHARACTER;

    val label: String
        get() = if (this == WORD) "WER" else "CER"
}

/**
 * Word- or character-error rate for one utterance, plus the raw counts.
 *
 * [errorPercent] is a **percentage**, not a ratio, because the threshold it is
 * compared against is written in percentage points: `model-selection.md` allows
 * "WER regression vs the larger model in family ≤ +2 absolute", and "absolute"
 * means two percentage points, not two times the error rate. Storing a ratio here
 * would make that comparison off by a factor of a hundred.
 *
 * [referenceUnits] is kept because an error rate without its denominator is not
 * interpretable: 3 errors is a different statement about a model depending on
 * whether the reference held three words or three hundred.
 */
data class ErrorRate(
    val unit: ScoreUnit,
    val referenceUnits: Int,
    val substitutions: Int,
    val deletions: Int,
    val insertions: Int,
    val errorPercent: Double,
) {
    val errors: Int get() = substitutions + deletions + insertions
}

/**
 * Word/character error rate, and the normalization that makes it comparable
 * across model families.
 *
 * The normalization is not cosmetic. sherpa-onnx's families disagree about
 * surface form in ways that have nothing to do with recognition quality, and
 * scoring them without settling these first measures their output formatting:
 *
 * - **Zipformer transducers emit UPPERCASE.** `stream/zipformer` models ship
 *   `tokens.txt` in capitals and return `HELLO WORLD`. LibriSpeech references are
 *   lower case. Left alone, every token is a substitution and WER sits near 100%
 *   for a model that is working perfectly.
 * - **Whisper emits punctuation and casing**, and its `tiny.en` variant is
 *   trained on lower-cased, unpunctuated text, so it disagrees with the
 *   multilingual variant on the same audio. Punctuation is therefore removed
 *   rather than normalized to a canonical form, because there is no single
 *   canonical form to normalize *to*.
 * - **SenseVoice decorates its output with rich-transcription tags** —
 *   `<|en|><|NEUTRAL|><|Speech|><|woitn|>the text`. sherpa-onnx strips these in
 *   some code paths and not others depending on version, so they are stripped
 *   here unconditionally. Leaving them in counts every tag as a word.
 *
 * Digits are left alone. Converting `3` to `three` would be a real
 * normalization, but it is one no published WER applies, and a score that is not
 * comparable to anyone else's number is worth less than a slightly cruder one.
 */
object WordErrorRate {

    /** SenseVoice rich-transcription tags, e.g. `<|en|>`, `<|NEUTRAL|>`. */
    private val RICH_TAG = Regex("<\\|[^|]*\\|>")

    /**
     * Reduces a hypothesis or reference to the form the score is computed on.
     *
     * Exposed rather than private because a run that reports a WER nobody can
     * reproduce is not a measurement, and reproducing it means seeing exactly what
     * the scorer saw.
     */
    fun normalize(text: String, unit: ScoreUnit): String {
        val stripped = RICH_TAG.replace(text, " ").lowercase()
        // Punctuation and symbols become separators, then runs of whitespace
        // collapse. For CHARACTER scoring the separators are dropped instead, so
        // `你好, 世界` and `你好世界` score identically — which they are.
        val cleaned = buildString {
            var pendingSeparator = false
            for (ch in stripped) {
                val isSeparator = ch.isWhitespace() || isPunctuationOrSymbol(ch)
                when {
                    unit == ScoreUnit.CHARACTER && isSeparator -> Unit
                    isSeparator -> {
                        pendingSeparator = true
                    }
                    pendingSeparator -> {
                        append(' ')
                        pendingSeparator = false
                        append(ch)
                    }
                    else -> append(ch)
                }
            }
        }
        return cleaned.trim().replace(WHITESPACE_RUN, " ")
    }

    private val WHITESPACE_RUN = Regex("\\s+")

    /**
     * Punctuation, symbols, and the CJK-adjacent marks that carry no lexical
     * content. `isLetterOrDigit()` is the discriminator rather than an explicit
     * punctuation table: anything that is neither a letter nor a digit is a
     * separator, which is exactly the rule a word-level score wants and stays
     * right for scripts this function has never seen.
     */
    private fun isPunctuationOrSymbol(ch: Char): Boolean = !ch.isLetterOrDigit()

    /**
     * Score one utterance. Returns `null` when the reference normalizes to
     * nothing, because the error rate would be a division by zero and any value
     * invented for it — 0, 100, infinity — is a number nobody measured.
     */
    fun score(
        hypothesis: String,
        reference: String,
        unit: ScoreUnit,
    ): ErrorRate? {
        // Both tokenizations are `List<String>` rather than left to the `if`, because
        // `split` yields `List<String>` and `toList` on a `CharSequence` yields
        // `List<Char>`: written as one `let`, Kotlin infers a common supertype that is
        // neither, and the failure surfaces two call sites away at `editDistance`.
        val ref: List<String> =
            normalize(reference, unit).let { if (unit == ScoreUnit.WORD) it.split(' ') else it.map(Char::toString) }
                .filter { it.isNotEmpty() }
        if (ref.isEmpty()) return null

        val hyp: List<String> =
            normalize(hypothesis, unit).let { if (unit == ScoreUnit.WORD) it.split(' ') else it.map(Char::toString) }
                .filter { it.isNotEmpty() }

        val (subs, dels, ins) = editDistance(ref, hyp)
        val errors = subs + dels + ins
        return ErrorRate(
            unit = unit,
            referenceUnits = ref.size,
            substitutions = subs,
            deletions = dels,
            insertions = ins,
            errorPercent = 100.0 * errors / ref.size,
        )
    }

    /**
     * Corpus-level rate: **total errors over total reference units**, not the mean
     * of the per-utterance rates.
     *
     * Those differ, and the difference matters at this corpus size. The mean of
     * rates weights a two-word utterance equally with a thirty-second one, so it
     * is dominated by short utterances and by any single one that went badly — and
     * with ~20 utterances per cell one bad decode can move it several points. The
     * aggregate is the number every published WER reports, so it is the number
     * this reports.
     *
     * `null` when any utterance could not be scored, rather than dropping it: a
     * corpus score computed over a subset reads exactly like a whole-corpus score,
     * and a silently shortened corpus is the easiest way to make a model look
     * better than it is.
     */
    fun aggregate(rates: List<ErrorRate>): ErrorRate? {
        if (rates.isEmpty()) return null
        val unit = rates.first().unit
        require(rates.all { it.unit == unit }) { "cannot aggregate ${unit.label} with ${rates.first { it.unit != unit }.unit.label}" }
        val refUnits = rates.sumOf { it.referenceUnits }
        val subs = rates.sumOf { it.substitutions }
        val dels = rates.sumOf { it.deletions }
        val ins = rates.sumOf { it.insertions }
        return ErrorRate(
            unit = unit,
            referenceUnits = refUnits,
            substitutions = subs,
            deletions = dels,
            insertions = ins,
            errorPercent = 100.0 * (subs + dels + ins) / refUnits,
        )
    }

    /**
     * Levenshtein alignment with substitutions, deletions and insertions counted
     * separately.
     *
     * Three counters rather than one distance because a model that **omits** words
     * and one that **hallucinates** them fail differently, and a dictation app
     * feels those differently: dropped words lose the user's content, inserted
     * words put words in their mouth that they never said, which is worse in a
     * tool whose whole claim is that it does not editorialize. Reporting only the
     * total hides that.
     *
     * Two rolling rows rather than an `n × m` table. A 30-second utterance is
     * ~480,000 reference units in the worst case and the full matrix would be
     * hundreds of millions of cells; the rolling form is O(min(n, m)) memory and
     * identical in result.
     */
    internal fun editDistance(reference: List<String>, hypothesis: List<String>): Triple<Int, Int, Int> {
        var prev = IntArray(hypothesis.size + 1) { it }
        var curr = IntArray(hypothesis.size + 1)

        for (i in 1..reference.size) {
            curr[0] = i
            for (j in 1..hypothesis.size) {
                curr[j] = if (reference[i - 1] == hypothesis[j - 1]) {
                    prev[j - 1]
                } else {
                    // substitution, deletion, insertion
                    minOf(prev[j - 1], curr[j - 1], prev[j]) + 1
                }
            }
            val swap = prev
            prev = curr
            curr = swap
        }

        // Recover the operation mix by walking one more pass over the same
        // recurrence, this time keeping the choice. The distance alone is already
        // known from the loop above; this pass exists only to attribute it, and it
        // is O(n·m) time and O(m) memory like the first.
        val back = Array(reference.size + 1) { IntArray(hypothesis.size + 1) }
        for (i in 0..reference.size) {
            for (j in 0..hypothesis.size) {
                back[i][j] = when {
                    i == 0 && j == 0 -> 0
                    i == 0 -> 2 // insertion
                    j == 0 -> 1 // deletion
                    reference[i - 1] == hypothesis[j - 1] -> back[i - 1][j - 1]
                    else -> when (minOf(back[i - 1][j - 1], back[i - 1][j], back[i][j - 1])) {
                        back[i - 1][j - 1] -> 0 // substitution
                        back[i - 1][j] -> 1 // deletion
                        else -> 2 // insertion
                    }
                }
            }
        }

        var subs = 0
        var dels = 0
        var ins = 0
        var i = reference.size
        var j = hypothesis.size
        while (i > 0 || j > 0) {
            when (back[i][j]) {
                0 -> { if (i > 0 && j > 0 && reference[i - 1] != hypothesis[j - 1]) subs++; i--; j-- }
                1 -> { dels++; i-- }
                else -> { ins++; j-- }
            }
        }
        return Triple(subs, dels, ins)
    }
}
