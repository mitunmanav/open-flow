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

    /**
     * Gravity.BOTTOM y: lift so the bubble sits above the IME.
     * If IME is down ([imeHeightPx] ≤ 0) keep [y].
     */
    fun parkYAboveIme(y: Int, imeHeightPx: Int, gapPx: Int = 24): Int {
        if (imeHeightPx <= 0) return y
        return maxOf(y, imeHeightPx + gapPx)
    }

    /** IME window height from screen bounds. Bad rect → 0. Clamped to screen. */
    fun imeHeightFromBounds(top: Int, bottom: Int, screenHeightPx: Int): Int {
        if (bottom <= top) return 0
        return (bottom - top).coerceIn(0, screenHeightPx.coerceAtLeast(0))
    }

    /** Overlay window px. WRAP_CONTENT measures against the screen — pin a bar. */
    fun overlaySizePx(
        listening: Boolean,
        density: Float,
        shape: String = "pill"
    ): Pair<Int, Int> {
        val barH = (48f * density).toInt()
        if (listening) {
            return (220f * density).toInt() to barH
        }
        return when (shape) {
            "pill" -> (96f * density).toInt() to barH
            "dot" -> {
                val side = (28f * density).toInt()
                side to side
            }
            else -> {
                val side = (48f * density).toInt()
                side to side
            }
        }
    }
}
