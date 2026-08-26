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

    @Test
    fun empty_dictionary_keeps_confidence_behavior() {
        val hyps = listOf("weak", "strong")
        val scores = floatArrayOf(0.3f, 0.9f)
        assertThat(HypothesisPick.best(hyps, scores, dictionary = emptySet()))
            .isEqualTo("strong")
    }

    @Test
    fun dictionary_phrase_beats_higher_confidence() {
        val hyps = listOf("send it to john doe", "send it to Jon Doe")
        val scores = floatArrayOf(0.95f, 0.40f)
        assertThat(
            HypothesisPick.best(hyps, scores, dictionary = setOf("jon doe"))
        ).isEqualTo("send it to Jon Doe")
    }

    @Test
    fun dictionary_match_is_whole_word_only() {
        // "open" must not match inside "opening".
        val hyps = listOf("the opening slide", "open the deck")
        val scores = floatArrayOf(0.95f, 0.30f)
        assertThat(HypothesisPick.best(hyps, scores, dictionary = setOf("open")))
            .isEqualTo("open the deck")
    }

    @Test
    fun dictionary_casing_beats_case_insensitive_hit() {
        // Exact-cased dict form wins even at lower confidence.
        val hyps = listOf("email MITUN about it", "email Mitun about it")
        val scores = floatArrayOf(0.60f, 0.55f)
        assertThat(HypothesisPick.best(hyps, scores, dictionary = setOf("Mitun")))
            .isEqualTo("email Mitun about it")
    }

    @Test
    fun fuzzy_single_word_within_edit_distance_two_beats_confidence() {
        // "mitunn" is one letter off the dict term → fuzzy bonus outweighs confidence gap.
        val hyps = listOf("call mitunn now please", "nothing relevant here")
        val scores = floatArrayOf(0.90f, 0.95f)
        assertThat(HypothesisPick.best(hyps, scores, dictionary = setOf("mitun")))
            .isEqualTo("call mitunn now please")
    }

    @Test
    fun short_words_never_fuzzy_match() {
        // "cat"/"cap" distance 1 but len < 4 → no bonus either side.
        val hyps = listOf("a cat sat", "a cap sat")
        val scores = floatArrayOf(0.95f, 0.20f)
        assertThat(HypothesisPick.best(hyps, scores, dictionary = setOf("cap")))
            .isEqualTo("a cap sat")
    }

    @Test
    fun no_dictionary_match_anywhere_falls_back_to_confidence() {
        val hyps = listOf("plain words here", "other words there")
        val scores = floatArrayOf(0.80f, 0.10f)
        assertThat(HypothesisPick.best(hyps, scores, dictionary = setOf("jon doe")))
            .isEqualTo("plain words here")
    }

    @Test
    fun more_dictionary_phrases_win_over_fewer() {
        val hyps = listOf("mitun and sarvam", "mitun alone")
        val scores = floatArrayOf(0.5f, 0.5f)
        assertThat(
            HypothesisPick.best(hyps, scores, dictionary = setOf("mitun", "sarvam"))
        ).isEqualTo("mitun and sarvam")
    }

    @Test
    fun applySpans_swaps_alternative_when_it_hits_dictionary() {
        val base = "email mittun about the plan"
        val spans = listOf(
            HypothesisPick.SpanAlts(start = 6, end = 12, alternatives = listOf("Mitun"))
        )
        assertThat(HypothesisPick.applySpans(base, spans, setOf("mitun")))
            .isEqualTo("email Mitun about the plan")
    }

    @Test
    fun applySpans_skips_when_segment_already_matches() {
        val base = "email Mitun about the plan"
        val spans = listOf(
            HypothesisPick.SpanAlts(start = 6, end = 11, alternatives = listOf("mittens"))
        )
        assertThat(HypothesisPick.applySpans(base, spans, setOf("mitun")))
            .isEqualTo(base)
    }

    @Test
    fun applySpans_no_dict_or_spans_returns_base() {
        val spans = listOf(HypothesisPick.SpanAlts(0, 4, listOf("x")))
        assertThat(HypothesisPick.applySpans("keep me", spans, emptySet()))
            .isEqualTo("keep me")
        assertThat(HypothesisPick.applySpans("keep me", emptyList(), setOf("keep")))
            .isEqualTo("keep me")
    }

    @Test
    fun applySpans_multiple_ranges_right_to_left() {
        val base = "mittun met peterr"
        val spans = listOf(
            HypothesisPick.SpanAlts(11, 17, listOf("Peter")),
            HypothesisPick.SpanAlts(0, 6, listOf("Mitun")),
        )
        assertThat(HypothesisPick.applySpans(base, spans, setOf("mitun", "peter")))
            .isEqualTo("Mitun met Peter")
    }

    @Test
    fun golden_hypothesis_list_meets_recall() {
        val f = java.io.File("src/test/resources/golden/hypotheses.txt")
        assertThat(f.isFile).isTrue()
        var dictHits = 0
        var dictCases = 0
        for (line in f.readLines(Charsets.UTF_8)) {
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) continue
            val p = t.split(" ||| ")
            assertThat(p.size).isEqualTo(5)
            val dict = p[0].split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val hyps = listOf(p[1], p[2])
            val scores = p[3].split(",").map { it.trim().toFloat() }.toFloatArray()
            val expected = p[4]
            val got = HypothesisPick.best(hyps, scores, dictionary = dict)
            assertThat(got).isEqualTo(expected)
            dictCases++
            if (dict.any { expected.contains(it, ignoreCase = true) }) dictHits++
        }
        assertThat(dictCases).isAtLeast(5)
        assertThat(dictHits.toFloat() / dictCases).isAtLeast(0.90f)
    }
}
