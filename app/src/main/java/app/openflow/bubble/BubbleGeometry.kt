package app.openflow.bubble

/**
 * Pure layout/RMS helpers for the Flow Bubble overlay.
 * WindowManager uses Gravity.END|BOTTOM → [x] is offset from the right edge.
 */
object BubbleGeometry {
    fun snapOffsetFromEnd(
        x: Int,
        screenWidthPx: Int,
        bubbleWidthPx: Int,
        marginPx: Int = 32
    ): Int {
        val right = marginPx
        val left = (screenWidthPx - bubbleWidthPx - marginPx).coerceAtLeast(marginPx)
        val mid = (left + right) / 2
        return if (x <= mid) right else left
    }

    /** Map SpeechRecognizer rmsdB (~0–10) → gentle scale pulse 0.95–1.05. */
    fun rmsScaleY(rmsdB: Float, minDb: Float = 0f, maxDb: Float = 10f): Float {
        val span = (maxDb - minDb).takeIf { it > 0f } ?: 1f
        val t = ((rmsdB - minDb) / span).coerceIn(0f, 1f)
        return 0.95f + t * 0.10f
    }
}
