package app.openflow.insights

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.TimeZone

class InsightsAggregatePolicyTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun wpm_uses_duration_and_wordCount() {
        val s = listOf(
            InsightSession("a b c d", "", 0L, durationMs = 60_000, wordCount = 4),
            InsightSession("x", "", 0L, durationMs = 0, wordCount = 1),
        )
        assertThat(InsightsAggregatePolicy.wordsPerMinute(s)).isWithin(0.01).of(4.0)
    }

    @Test
    fun cleaned_delta_only_when_raw_present() {
        val s = listOf(
            InsightSession("hi there", "hi there um", 0L, 1000, 2),
            InsightSession("ok", "", 0L, 1000, 1),
        )
        assertThat(InsightsAggregatePolicy.cleanedDeltaWords(s)).isEqualTo(1)
    }

    @Test
    fun peak_hour_is_mode() {
        val s = listOf(
            InsightSession("a", "", 3 * 3_600_000L, 1000, 1),
            InsightSession("b", "", 3 * 3_600_000L + 10, 1000, 1),
            InsightSession("c", "", 5 * 3_600_000L, 1000, 1),
        )
        assertThat(InsightsAggregatePolicy.peakHour(s, utc)).isEqualTo(3)
    }

    @Test
    fun top_word_skips_stopwords() {
        val s = listOf(InsightSession("the the flow flow flow is cool", "", 0L, 1000, 7))
        assertThat(InsightsAggregatePolicy.topWord(s)).isEqualTo("flow")
    }

    @Test
    fun most_corrected_from_raw_vs_clean() {
        val s = listOf(
            InsightSession("hello world", "helo world", 0L, 1000, 2),
            InsightSession("hello", "helo", 0L, 1000, 1),
        )
        assertThat(InsightsAggregatePolicy.mostCorrectedToken(s)).isEqualTo("helo")
    }

    @Test
    fun unlock_at_2000() {
        assertThat(InsightsAggregatePolicy.voiceUnlocked(1999)).isFalse()
        assertThat(InsightsAggregatePolicy.voiceUnlocked(2000)).isTrue()
    }

    @Test
    fun byok_payload_has_no_transcript_fields() {
        val s = listOf(
            InsightSession(
                text = "alpha beta gamma",
                rawText = "alpha beta gamma um",
                createdAtEpochMs = 0L,
                durationMs = 60_000,
                wordCount = 3,
                packageName = "com.example.app",
            )
        )
        val p = InsightsAggregatePolicy.byokPayload(s, totalWords = 2500, streakDays = 2, zone = utc)
        assertThat(p.keys).containsAtLeast(
            "totalWords", "sessions", "wpm", "streakDays", "cleanedDeltaWords",
            "peakHour", "topWords", "topPackages", "mostCorrected",
        )
        assertThat(p.containsKey("text")).isFalse()
        assertThat(p.containsKey("rawText")).isFalse()
        assertThat(p.containsKey("transcript")).isFalse()
        // aggregates may list top words; never ship full session bodies
        assertThat(p.values.any { it is String && it.contains("alpha beta gamma") }).isFalse()
    }

    @Test
    fun heatmap_covers_12_weeks_buckets() {
        val now = 12L * 7 * 86_400_000L
        val s = listOf(InsightSession("hi", "", now - 1000, 1000, 2))
        val days = InsightsAggregatePolicy.dayWordCounts(s, nowMs = now, zone = utc, weeks = 12)
        assertThat(days.size).isEqualTo(12 * 7)
        assertThat(days.sumOf { it.words }).isEqualTo(2)
    }
}
