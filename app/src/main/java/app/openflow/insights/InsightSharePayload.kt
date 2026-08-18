package app.openflow.insights

/**
 * Local-only insight share text. Aggregates — no transcripts.
 */
object InsightSharePayload {
    fun text(
        totalWords: Long,
        totalSessions: Long,
        streakDays: Int,
        wpm: Double,
    ): String = buildString {
        appendLine("Open Flow — my voice stats")
        appendLine()
        appendLine("Words: $totalWords")
        appendLine("Sessions: $totalSessions")
        appendLine("Streak: $streakDays days")
        appendLine("Speed: ${"%.0f".format(wpm)} WPM")
        appendLine()
        append("Local on my phone. No cloud.")
    }
}
