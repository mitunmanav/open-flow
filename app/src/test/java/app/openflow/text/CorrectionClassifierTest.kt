package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CorrectionClassifierTest {

    private fun signals(
        timeMs: Long = 10_000L,
        distance: Int = 1,
        insertedWords: Int = 8,
        editedWords: Int = 8,
        overlap: Double = 0.85,
        altUsed: Boolean = false
    ) = CorrectionClassifier.Signals(
        timeSinceInsertionMs = timeMs,
        editDistanceWords = distance,
        insertedWordCount = insertedWords,
        editedWordCount = editedWords,
        wordOverlap = overlap,
        alternativeUsed = altUsed
    )

    // -- strong signal: alternatives -----------------------------------------

    @Test
    fun alternative_used_is_correction_even_late_and_large() {
        val s = signals(timeMs = 600_000L, distance = 4, overlap = 0.3, altUsed = true)
        assertThat(CorrectionClassifier.classify(s))
            .isEqualTo(CorrectionClassifier.Kind.CORRECTION)
    }

    // -- corrections ----------------------------------------------------------

    @Test
    fun quick_small_fix_is_correction() {
        val s = signals(timeMs = 5_000L, distance = 1, overlap = 0.9)
        assertThat(CorrectionClassifier.classify(s))
            .isEqualTo(CorrectionClassifier.Kind.CORRECTION)
    }

    @Test
    fun two_word_swap_quick_is_correction() {
        val s = signals(timeMs = 20_000L, distance = 2, insertedWords = 10, editedWords = 10, overlap = 0.8)
        assertThat(CorrectionClassifier.classify(s))
            .isEqualTo(CorrectionClassifier.Kind.CORRECTION)
    }

    @Test
    fun moderate_time_close_texts_still_correction() {
        val s = signals(timeMs = 60_000L, distance = 2, overlap = 0.9)
        assertThat(CorrectionClassifier.classify(s))
            .isEqualTo(CorrectionClassifier.Kind.CORRECTION)
    }

    // -- edits ----------------------------------------------------------------

    @Test
    fun huge_rewrite_is_edit() {
        val s = signals(distance = 7, insertedWords = 8, editedWords = 9, overlap = 0.15)
        assertThat(CorrectionClassifier.classify(s))
            .isEqualTo(CorrectionClassifier.Kind.EDIT)
    }

    @Test
    fun pure_continuation_growth_is_edit() {
        val s = signals(distance = 12, insertedWords = 6, editedWords = 18, overlap = 1.0)
        assertThat(CorrectionClassifier.classify(s))
            .isEqualTo(CorrectionClassifier.Kind.EDIT)
    }

    @Test
    fun late_low_overlap_touch_is_edit() {
        val s = signals(timeMs = 300_000L, distance = 3, overlap = 0.4)
        assertThat(CorrectionClassifier.classify(s))
            .isEqualTo(CorrectionClassifier.Kind.EDIT)
    }

    @Test
    fun default_uncertain_is_edit() {
        val s = signals(timeMs = 180_000L, distance = 2, overlap = 0.55)
        assertThat(CorrectionClassifier.classify(s))
            .isEqualTo(CorrectionClassifier.Kind.EDIT)
    }

    // -- signalsFor: build Signals from raw texts ------------------------------

    @Test
    fun signalsFor_counts_distance_and_overlap() {
        val s = CorrectionClassifier.signalsFor(
            inserted = "meet me at the cafe tomorrow",
            edited = "meet me at the diner tomorrow",
            timeSinceInsertionMs = 8_000L,
        )
        assertThat(s.insertedWordCount).isEqualTo(6)
        assertThat(s.editedWordCount).isEqualTo(6)
        assertThat(s.editDistanceWords).isEqualTo(1)
        assertThat(s.wordOverlap).isWithin(1e-9).of(5.0 / 6.0)
        assertThat(s.timeSinceInsertionMs).isEqualTo(8_000L)
        assertThat(s.alternativeUsed).isFalse()
    }

    @Test
    fun signalsFor_identical_texts_zero_distance_full_overlap() {
        val s = CorrectionClassifier.signalsFor("Hello world", "hello world!", 1_000L)
        assertThat(s.editDistanceWords).isEqualTo(0)
        assertThat(s.wordOverlap).isWithin(1e-9).of(1.0)
    }

    @Test
    fun signalsFor_growth_detected() {
        val s = CorrectionClassifier.signalsFor("one two three", "one two three four five six seven", 2_000L)
        assertThat(s.insertedWordCount).isEqualTo(3)
        assertThat(s.editedWordCount).isEqualTo(7)
        assertThat(s.editDistanceWords).isEqualTo(4)
        assertThat(s.wordOverlap).isWithin(1e-9).of(3.0 / 7.0)
    }

    @Test
    fun signalsFor_empty_inserted_is_total_change() {
        val s = CorrectionClassifier.signalsFor("", "fresh words here", 1_000L)
        assertThat(s.insertedWordCount).isEqualTo(0)
        assertThat(s.editDistanceWords).isEqualTo(3)
        assertThat(s.wordOverlap).isWithin(1e-9).of(0.0)
    }

    // -- synthetic eval: precision >= 90% --------------------------------------

    private data class Case(val s: CorrectionClassifier.Signals, val truthIsCorrection: Boolean)

    private fun syntheticSet(): List<Case> = listOf(
        // true corrections (fast small fixes, phonetic-style swaps)
        Case(signals(timeMs = 3_000L, distance = 1, overlap = 0.92), true),
        Case(signals(timeMs = 8_000L, distance = 1, overlap = 0.88), true),
        Case(signals(timeMs = 15_000L, distance = 2, overlap = 0.82), true),
        Case(signals(timeMs = 30_000L, distance = 1, overlap = 0.95), true),
        Case(signals(timeMs = 45_000L, distance = 2, overlap = 0.85), true),
        Case(signals(timeMs = 10_000L, distance = 2, overlap = 0.78), true),
        Case(signals(timeMs = 25_000L, distance = 1, overlap = 0.9), true),
        Case(signals(timeMs = 50_000L, distance = 2, overlap = 0.87), true),
        Case(signals(timeMs = 5_000L, distance = 3, overlap = 0.75), true),
        Case(signals(altUsed = true), true),
        Case(signals(timeMs = 120_000L, altUsed = true), true),
        Case(signals(timeMs = 400_000L, distance = 2, overlap = 0.9), true),
        // true edits (rephrasing, continuations, late rewrites)
        Case(signals(timeMs = 200_000L, distance = 8, overlap = 0.2), false),
        Case(signals(timeMs = 300_000L, distance = 5, overlap = 0.35), false),
        Case(signals(timeMs = 60_000L, distance = 14, insertedWords = 6, editedWords = 20, overlap = 1.0), false),
        Case(signals(timeMs = 90_000L, distance = 6, overlap = 0.3), false),
        Case(signals(timeMs = 150_000L, distance = 4, overlap = 0.5), false),
        Case(signals(timeMs = 240_000L, distance = 3, overlap = 0.45), false),
        Case(signals(timeMs = 100_000L, distance = 9, insertedWords = 10, editedWords = 11, overlap = 0.25), false),
        Case(signals(timeMs = 500_000L, distance = 2, overlap = 0.6), false),
        Case(signals(timeMs = 70_000L, distance = 5, overlap = 0.4), false),
        Case(signals(timeMs = 120_000L, distance = 10, insertedWords = 12, editedWords = 13, overlap = 0.2), false)
    )

    @Test
    fun synthetic_precision_at_least_90_percent() {
        val cases = syntheticSet()
        val predictedCorrections = cases.filter {
            CorrectionClassifier.classify(it.s) == CorrectionClassifier.Kind.CORRECTION
        }
        assertThat(predictedCorrections).isNotEmpty()
        val precision = predictedCorrections.count { it.truthIsCorrection } /
            predictedCorrections.size.toDouble()
        assertThat(precision).isAtLeast(0.90)
    }
}
