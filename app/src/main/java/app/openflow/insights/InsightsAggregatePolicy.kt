package app.openflow.insights

import java.util.Calendar
import java.util.TimeZone

data class InsightSession(
    val text: String,
    val rawText: String,
    val createdAtEpochMs: Long,
    val durationMs: Long,
    val wordCount: Int,
    val packageName: String = "",
)

data class DayBucket(
    val dayEpoch: Long,
    val words: Int,
)

/**
 * Pure Usage + Voice aggregates. No network. No transcripts in [byokPayload].
 */
object InsightsAggregatePolicy {
    const val VOICE_UNLOCK_WORDS = 2000L

    private val WORD_SPLIT = Regex("\\s+")
    private val TOKEN_SPLIT = Regex("[^a-z0-9']+")

    fun wordsPerMinute(sessions: List<InsightSession>): Double {
        var words = 0
        var ms = 0L
        for (s in sessions) {
            if (s.durationMs <= 0L) continue
            words += s.wordCount
            ms += s.durationMs
        }
        if (ms <= 0L) return 0.0
        return words / (ms / 60_000.0)
    }

    fun cleanedDeltaWords(sessions: List<InsightSession>): Int {
        var sum = 0
        for (s in sessions) {
            if (s.rawText.isBlank()) continue
            val rawN = countWords(s.rawText)
            val cleanN = countWords(s.text)
            sum += maxOf(0, rawN - cleanN)
        }
        return sum
    }

    fun dayWordCounts(
        sessions: List<InsightSession>,
        nowMs: Long,
        zone: TimeZone,
        weeks: Int = 12,
    ): List<DayBucket> {
        val days = (weeks * 7).coerceAtLeast(1)
        val endDay = startOfLocalDay(nowMs, zone)
        val startDay = endDay - (days - 1L) * 86_400_000L
        val counts = LinkedHashMap<Long, Int>(days)
        var d = startDay
        repeat(days) {
            counts[d] = 0
            d += 86_400_000L
        }
        for (s in sessions) {
            val day = startOfLocalDay(s.createdAtEpochMs, zone)
            if (day in counts) {
                counts[day] = (counts[day] ?: 0) + s.wordCount
            }
        }
        return counts.map { DayBucket(it.key, it.value) }
    }

    fun peakHour(sessions: List<InsightSession>, zone: TimeZone): Int? {
        if (sessions.isEmpty()) return null
        val cal = Calendar.getInstance(zone)
        val freq = IntArray(24)
        for (s in sessions) {
            cal.timeInMillis = s.createdAtEpochMs
            freq[cal.get(Calendar.HOUR_OF_DAY)]++
        }
        var bestH = 0
        var bestN = -1
        for (h in 0 until 24) {
            if (freq[h] > bestN) {
                bestN = freq[h]
                bestH = h
            }
        }
        return if (bestN <= 0) null else bestH
    }

    fun topWord(
        sessions: List<InsightSession>,
        stopwords: Set<String> = InsightsStopwords.EN,
    ): String? {
        val freq = HashMap<String, Int>()
        for (s in sessions) {
            for (t in tokens(s.text)) {
                if (t in stopwords) continue
                if (t.length < 2) continue
                freq[t] = (freq[t] ?: 0) + 1
            }
        }
        return freq.maxByOrNull { it.value }?.key
    }

    fun mostCorrectedToken(sessions: List<InsightSession>): String? {
        val extra = HashMap<String, Int>()
        for (s in sessions) {
            if (s.rawText.isBlank()) continue
            val rawBag = bag(tokens(s.rawText))
            val cleanBag = bag(tokens(s.text))
            for ((tok, n) in rawBag) {
                val left = n - (cleanBag[tok] ?: 0)
                if (left > 0) extra[tok] = (extra[tok] ?: 0) + left
            }
        }
        return extra.maxByOrNull { it.value }?.key
    }

    fun topPackage(sessions: List<InsightSession>): String? {
        val freq = HashMap<String, Int>()
        for (s in sessions) {
            val p = s.packageName.trim()
            if (p.isEmpty()) continue
            freq[p] = (freq[p] ?: 0) + 1
        }
        return freq.maxByOrNull { it.value }?.key
    }

    fun voiceUnlocked(totalWords: Long): Boolean = totalWords >= VOICE_UNLOCK_WORDS

    fun byokPayload(
        sessions: List<InsightSession>,
        totalWords: Long,
        streakDays: Int,
        zone: TimeZone,
    ): Map<String, Any?> {
        val topWords = topNWords(sessions, n = 10)
        val topPackages = topNPackages(sessions, n = 5)
        return linkedMapOf(
            "totalWords" to totalWords,
            "sessions" to sessions.size,
            "wpm" to wordsPerMinute(sessions),
            "streakDays" to streakDays,
            "cleanedDeltaWords" to cleanedDeltaWords(sessions),
            "peakHour" to peakHour(sessions, zone),
            "topWords" to topWords,
            "topPackages" to topPackages,
            "mostCorrected" to mostCorrectedToken(sessions),
        )
    }

    private fun topNWords(sessions: List<InsightSession>, n: Int): List<String> {
        val freq = HashMap<String, Int>()
        for (s in sessions) {
            for (t in tokens(s.text)) {
                if (t in InsightsStopwords.EN) continue
                if (t.length < 2) continue
                freq[t] = (freq[t] ?: 0) + 1
            }
        }
        return freq.entries.sortedByDescending { it.value }.take(n).map { it.key }
    }

    private fun topNPackages(sessions: List<InsightSession>, n: Int): List<String> {
        val freq = HashMap<String, Int>()
        for (s in sessions) {
            val p = s.packageName.trim()
            if (p.isEmpty()) continue
            freq[p] = (freq[p] ?: 0) + 1
        }
        return freq.entries.sortedByDescending { it.value }.take(n).map { it.key }
    }

    private fun countWords(s: String): Int =
        s.trim().split(WORD_SPLIT).filter { it.isNotEmpty() }.size

    private fun tokens(s: String): List<String> =
        s.lowercase().split(TOKEN_SPLIT).filter { it.isNotEmpty() }

    private fun bag(tokens: List<String>): Map<String, Int> {
        val m = HashMap<String, Int>()
        for (t in tokens) m[t] = (m[t] ?: 0) + 1
        return m
    }

    private fun startOfLocalDay(epochMs: Long, zone: TimeZone): Long {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = epochMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
