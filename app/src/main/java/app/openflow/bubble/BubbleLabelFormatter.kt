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

    private fun softCap(text: String, maxChars: Int): String {
        if (maxChars < 4 || text.length <= maxChars) return text
        return text.take(maxChars - 1) + "…"
    }
}
