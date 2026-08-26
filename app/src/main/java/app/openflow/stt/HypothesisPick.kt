package app.openflow.stt

import kotlin.math.abs
import kotlin.math.min

/**
 * Pick best SpeechRecognizer hypothesis.
 *
 * [android.speech.SpeechRecognizer.CONFIDENCE_SCORES] (API 14+; still the API 34
 * confidence extra): same length as RESULTS_RECOGNITION, 0.0–1.0, or -1 if missing.
 *
 * With a [dictionary] (learned dict values + focused-field tokens), hypotheses are
 * re-ranked: each whole-word phrase hit adds [DICT_BONUS]. Whole-word only —
 * PRISM-style no-prefix rule ("open" never matches "opening"). Single words of
 * length >= 4 also fuzzy-match within edit distance 2 (ASR near-miss tolerance).
 * No dictionary → pure confidence; ties → earlier hypothesis.
 */
object HypothesisPick {

    /** Per matched dictionary phrase. Beats any realistic confidence delta. */
    private const val EXACT_CASE_BONUS = 2f
    private const val IGNORE_CASE_BONUS = 1.5f
    private const val FUZZY_BONUS = 0.6f
    private const val FUZZY_MIN_LEN = 4
    private const val FUZZY_MAX_DISTANCE = 2

    /** Char range + alternative spellings the recognizer offers for that range. */
    data class SpanAlts(val start: Int, val end: Int, val alternatives: List<String>)

    fun best(
        hypotheses: List<String>?,
        scores: FloatArray?,
        preferFormatted: Boolean = false,
        dictionary: Set<String> = emptySet(),
    ): String {
        val raw = hypotheses ?: return ""
        if (raw.isEmpty()) return ""
        // Formatted extras rarely carry usable scores — treat them as absent.
        val usableScores = if (preferFormatted) null else validScores(raw.size, scores)
        var bestIdx = -1
        var bestScore = Float.NEGATIVE_INFINITY
        for (i in raw.indices) {
            val text = raw[i].trim()
            if (text.isEmpty()) continue
            val score = usableScores?.getOrNull(i) ?: 0f
            val total = score + dictBonus(text, dictionary)
            if (total > bestScore) {
                bestScore = total
                bestIdx = i
            }
        }
        if (bestIdx >= 0) return raw[bestIdx].trim()
        return raw.firstOrNull { it.trim().isNotEmpty() }?.trim().orEmpty()
    }

    /**
     * Apply span alternatives to the picked text when an alternative hits the
     * dictionary exactly (case-insensitive) and the original segment does not.
     * Fuzzy never drives a swap — recognizer alternatives are already candidates.
     * Ranges processed right-to-left so earlier offsets stay valid.
     */
    fun applySpans(base: String, spans: List<SpanAlts>, dictionary: Set<String>): String {
        if (dictionary.isEmpty() || base.isEmpty() || spans.isEmpty()) return base
        val sb = StringBuilder(base)
        for (span in spans.sortedByDescending { it.start }) {
            val s = span.start.coerceIn(0, sb.length)
            val e = span.end.coerceIn(s, sb.length)
            if (s == e) continue
            val segment = sb.substring(s, e)
            val segHit = dictionary.any { matchesExactIgnoreCase(segment, it) }
            if (segHit) continue
            val alt = span.alternatives.firstOrNull { candidate ->
                candidate.isNotBlank() &&
                    !candidate.equals(segment, ignoreCase = true) &&
                    dictionary.any { matchesExactIgnoreCase(candidate, it) }
            } ?: continue
            sb.replace(s, e, alt.trim())
        }
        return sb.toString()
    }

    fun joinParts(parts: List<String>?): String =
        parts.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun validScores(size: Int, scores: FloatArray?): FloatArray? {
        if (scores == null || scores.size != size) return null
        return scores
    }

    /** Sum of dictionary bonuses present in [text]: exact > case-insensitive > fuzzy. */
    private fun dictBonus(text: String, dictionary: Set<String>): Float {
        if (dictionary.isEmpty() || text.isBlank()) return 0f
        var bonus = 0f
        for (entry in dictionary) {
            val term = entry.trim()
            if (term.isEmpty()) continue
            val padded = " ${text.trim()} "
            if (padded.contains(" $term ")) {
                bonus += EXACT_CASE_BONUS
                continue
            }
            val lowTerm = term.lowercase()
            if (padded.lowercase().contains(" $lowTerm ")) {
                bonus += IGNORE_CASE_BONUS
                continue
            }
            if (lowTerm.length < FUZZY_MIN_LEN || lowTerm.contains(' ')) continue
            if (words(text).any { w ->
                    w.length >= FUZZY_MIN_LEN && editDistance(w, lowTerm) <= FUZZY_MAX_DISTANCE
                }
            ) {
                bonus += FUZZY_BONUS
            }
        }
        return bonus
    }

    private fun matchesExactIgnoreCase(text: String, rawTerm: String): Boolean {
        val term = rawTerm.trim().lowercase()
        if (term.isEmpty() || text.isBlank()) return false
        return " ${text.lowercase()} ".contains(" $term ")
    }

    private fun words(text: String): List<String> =
        text.split(Regex("\\s+")).map { word ->
            word.trim(' ', ',', '.', '!', '?', ';', ':', '"', '\'', '(', ')').lowercase()
        }.filter { it.isNotEmpty() }

    private fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (abs(a.length - b.length) > FUZZY_MAX_DISTANCE) return Int.MAX_VALUE
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = min(
                    min(prev[j] + 1, cur[j - 1] + 1),
                    prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1,
                )
            }
            System.arraycopy(cur, 0, prev, 0, cur.size)
        }
        return prev[b.length]
    }
}
