package app.openflow.bubble

/**
 * Pure rules for floating bubble label text (live transcript).
 */
object BubbleLabelFormatter {

    fun idle(): String = "Tap"

    fun listening(elapsedSec: Long): String =
        if (elapsedSec > 0) "Hearing ${elapsedSec}s" else "Hearing…"

    fun partial(text: String, elapsedSec: Long = 0L, maxChars: Int = 80): String {
        val t = text.trim()
        if (t.isEmpty()) return listening(elapsedSec)
        return softCap(t, maxChars)
    }

    fun finalChunk(text: String, maxChars: Int = 80): String {
        val t = text.trim()
        if (t.isEmpty()) return "…"
        return softCap(t, maxChars)
    }

    fun needMic(): String = "Mic off"

    /**
     * User-facing ear error. Maps cloud HTTP codes so 403 isn't raw "cloud socket failed (403)".
     */
    fun earError(message: String, maxChars: Int = 48): String {
        val m = message.trim()
        if (m.isEmpty()) return "Speech error"
        val lower = m.lowercase()
        return when {
            "403" in lower || lower.contains("forbidden") ->
                "API key rejected — check Speech + AI"
            "401" in lower || lower.contains("unauthorized") ->
                "API key missing — open Speech + AI"
            lower.contains("cloud socket failed") ->
                softCap("Cloud STT failed — check key / net", maxChars)
            else -> softCap(m, maxChars)
        }
    }

    private fun softCap(text: String, maxChars: Int): String {
        if (maxChars < 4 || text.length <= maxChars) return text
        return text.take(maxChars - 1) + "…"
    }
}
