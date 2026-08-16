package app.openflow.stt

/**
 * Pick best SpeechRecognizer hypothesis.
 *
 * [android.speech.SpeechRecognizer.CONFIDENCE_SCORES] (API 14+; still the API 34
 * confidence extra): same length as RESULTS_RECOGNITION, 0.0–1.0, or -1 if missing.
 * Higher real score wins. No scores / all -1 / length mismatch → first non-blank.
 */
object HypothesisPick {

    fun best(
        hypotheses: List<String>?,
        scores: FloatArray?,
        preferFormatted: Boolean = false,
    ): String {
        val raw = hypotheses ?: return ""
        if (raw.isEmpty()) return ""
        if (preferFormatted) {
            return raw.firstOrNull { it.trim().isNotEmpty() }?.trim().orEmpty()
        }
        if (scores != null && scores.size == raw.size) {
            var bestIdx = -1
            var bestScore = Float.NEGATIVE_INFINITY
            for (i in raw.indices) {
                val text = raw[i].trim()
                if (text.isEmpty()) continue
                val score = scores[i]
                if (score < 0f) continue
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = i
                }
            }
            if (bestIdx >= 0) return raw[bestIdx].trim()
        }
        return raw.firstOrNull { it.trim().isNotEmpty() }?.trim().orEmpty()
    }

    fun joinParts(parts: List<String>?): String =
        parts.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
