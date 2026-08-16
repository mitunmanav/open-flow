package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HypothesisPickTest {

    @Test
    fun empty_or_blank_is_empty() {
        assertThat(HypothesisPick.best(null, null)).isEmpty()
        assertThat(HypothesisPick.best(emptyList(), null)).isEmpty()
        assertThat(HypothesisPick.best(listOf("  ", ""), null)).isEmpty()
    }

    @Test
    fun no_scores_takes_first_non_blank() {
        assertThat(HypothesisPick.best(listOf("  ", "hello", "world"), null))
            .isEqualTo("hello")
    }

    @Test
    fun higher_confidence_wins() {
        val hyps = listOf("Mitton", "Mitun", "mittens")
        val scores = floatArrayOf(0.40f, 0.91f, 0.70f)
        assertThat(HypothesisPick.best(hyps, scores)).isEqualTo("Mitun")
    }

    @Test
    fun unavailable_minus_one_scores_fall_back_to_first() {
        val hyps = listOf("first", "second")
        val scores = floatArrayOf(-1f, -1f)
        assertThat(HypothesisPick.best(hyps, scores)).isEqualTo("first")
    }

    @Test
    fun skips_unavailable_and_picks_real_score() {
        val hyps = listOf("weak", "strong")
        val scores = floatArrayOf(-1f, 0.8f)
        assertThat(HypothesisPick.best(hyps, scores)).isEqualTo("strong")
    }

    @Test
    fun score_size_mismatch_uses_first() {
        val hyps = listOf("alpha", "beta")
        assertThat(HypothesisPick.best(hyps, floatArrayOf(0.9f))).isEqualTo("alpha")
    }

    @Test
    fun tie_keeps_earlier_hypothesis() {
        val hyps = listOf("one", "two")
        val scores = floatArrayOf(0.5f, 0.5f)
        assertThat(HypothesisPick.best(hyps, scores)).isEqualTo("one")
    }

    @Test
    fun trims_winner() {
        val hyps = listOf("  hi there  ")
        assertThat(HypothesisPick.best(hyps, floatArrayOf(1f))).isEqualTo("hi there")
    }

    @Test
    fun formatting_pair_takes_first_not_higher_raw_score() {
        val hyps = listOf("Hello, world.", "hello world")
        val scores = floatArrayOf(0.40f, 0.99f)
        assertThat(HypothesisPick.best(hyps, scores, preferFormatted = true))
            .isEqualTo("Hello, world.")
        assertThat(HypothesisPick.best(hyps, scores, preferFormatted = false))
            .isEqualTo("hello world")
    }

    @Test
    fun joinParts_collapses_space() {
        assertThat(HypothesisPick.joinParts(listOf("  hello ", "", "world  ")))
            .isEqualTo("hello world")
        assertThat(HypothesisPick.joinParts(null)).isEmpty()
    }
}
