package app.openflow.bubble

/**
 * Pure layout, physics, and RMS helpers for the Flow Bubble overlay.
 * WindowManager uses Gravity.END|BOTTOM → [x] is offset from the right edge.
 */
object BubbleGeometry {

    const val DEFAULT_MARGIN_PX = 32

    enum class BubbleShape(val id: String, val label: String) {
        PILL("pill", "Pill"),
        CIRCLE("circle", "Circle"),
        SQUARE("square", "Squircle"),
        DOT("dot", "Dot")
    }

    /** Snap bubble to nearest screen edge (left or right) with margin. */
    fun snapOffsetFromEnd(
        x: Int,
        screenWidthPx: Int,
        bubbleWidthPx: Int,
        marginPx: Int = DEFAULT_MARGIN_PX
    ): Int {
        val right = marginPx
        val left = (screenWidthPx - bubbleWidthPx - marginPx).coerceAtLeast(marginPx)
        val mid = (left + right) / 2
        return if (x <= mid) right else left
    }

    /** Clamp Y position within screen safe boundaries (e.g. above navigation bar, below status bar). */
    fun clampVerticalOffset(
        y: Int,
        screenHeightPx: Int,
        bubbleHeightPx: Int,
        topMarginPx: Int = 120,
        bottomMarginPx: Int = 80
    ): Int {
        val minY = bottomMarginPx
        val maxY = (screenHeightPx - bubbleHeightPx - topMarginPx).coerceAtLeast(minY)
        return y.coerceIn(minY, maxY)
    }

    /** Map SpeechRecognizer rmsdB (~0–10) → gentle scale pulse 0.96–1.08 with smooth curve. */
    fun rmsScaleY(rmsdB: Float, minDb: Float = 0f, maxDb: Float = 10f): Float {
        val span = (maxDb - minDb).takeIf { it > 0f } ?: 1f
        val t = ((rmsdB - minDb) / span).coerceIn(0f, 1f)
        return 0.95f + t * 0.10f
    }

    /** Dynamic corner radius (px). Square = minimal hard 2dp. */
    fun cornerRadiusDp(shape: String, density: Float): Float =
        BubbleChrome.cornerPx(shape, density)
}
