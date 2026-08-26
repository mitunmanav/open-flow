package app.openflow.text

/**
 * Correction-vs-edit classifier for post-dictation user fixes.
 * Signals: time since insertion, word-level edit distance, word overlap,
 * STT-alternative usage. Precision-first: unsure -> EDIT (never learn).
 * Sources: Microsoft 2004 unsupervised correction learning; US8019602
 * (alternate-list pick = correction; late/large change = change of mind);
 * arxiv 2310.00141 (learn only likely misrecognitions).
 */
object CorrectionClassifier {

    enum class Kind { CORRECTION, EDIT }

    data class Signals(
        val timeSinceInsertionMs: Long,
        val editDistanceWords: Int,
        val insertedWordCount: Int,
        val editedWordCount: Int,
        /** shared-word fraction of the larger side, 0..1 */
        val wordOverlap: Double,
        /** edited text matches one of the recognizer's alternatives */
        val alternativeUsed: Boolean
    )

    private const val FAST_MS = 60_000L
    private const val LATE_MS = 120_000L

    /** Build [Signals] from raw texts + time. Alternative usage unknown here -> false. */
    fun signalsFor(inserted: String, edited: String, timeSinceInsertionMs: Long): Signals {
        val a = words(inserted)
        val b = words(edited)
        val shared = a.toSet().intersect(b.toSet()).size.toDouble()
        val overlap = if (maxOf(a.size, b.size) == 0) 0.0 else shared / maxOf(a.size, b.size)
        return Signals(
            timeSinceInsertionMs = timeSinceInsertionMs,
            editDistanceWords = editDistanceWords(a, b),
            insertedWordCount = a.size,
            editedWordCount = b.size,
            wordOverlap = overlap,
            alternativeUsed = false,
        )
    }

    fun classify(s: Signals): Kind {
        if (s.alternativeUsed) return Kind.CORRECTION

        val size = maxOf(s.insertedWordCount, s.editedWordCount)
        val changedRatio = if (size == 0) 1.0 else s.editDistanceWords.toDouble() / size
        if (s.wordOverlap < 0.35 || (changedRatio >= 0.6 && s.editDistanceWords >= 3)) {
            return Kind.EDIT
        }

        val growth = s.editedWordCount - s.insertedWordCount
        if (growth >= 3 && s.editDistanceWords >= growth) return Kind.EDIT

        if (s.timeSinceInsertionMs > LATE_MS && s.wordOverlap < 0.7) return Kind.EDIT

        if (s.timeSinceInsertionMs <= FAST_MS &&
            (s.editDistanceWords <= 2 || (s.editDistanceWords <= 3 && s.wordOverlap >= 0.7))
        ) {
            return Kind.CORRECTION
        }
        if (s.editDistanceWords <= 2 && s.wordOverlap >= 0.85) return Kind.CORRECTION

        return Kind.EDIT
    }

    private fun words(text: String): List<String> =
        Regex("[a-z0-9]+(?:'[a-z0-9]+)?").findAll(text.lowercase())
            .map { it.value }.filter { it.isNotBlank() }.toList()

    private fun editDistanceWords(a: List<String>, b: List<String>): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.size
        if (b.isEmpty()) return a.size
        val prev = IntArray(b.size + 1) { it }
        val cur = IntArray(b.size + 1)
        for (i in 1..a.size) {
            cur[0] = i
            for (j in 1..b.size) {
                cur[j] = minOf(
                    cur[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1,
                )
            }
            System.arraycopy(cur, 0, prev, 0, cur.size)
        }
        return prev[b.size]
    }
}
