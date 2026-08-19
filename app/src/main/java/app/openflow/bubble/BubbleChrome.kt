package app.openflow.bubble

/**
 * Minimal brutal chrome for the Flow Bubble overlay.
 * Cream / charcoal, hard edges by default; roundness prefs softens corners.
 */
object BubbleChrome {
    /** Charcoal face — matches [app.openflow.ui.theme.BrutalColors.Charcoal]. */
    const val IDLE_FILL = 0xFF1A1A1A.toInt()
    const val LISTEN_FILL = 0xFF1A1A1A.toInt()

    /** Cream stroke/icon — high contrast on dark + light host apps. */
    const val IDLE_STROKE = 0xFFF4F1EA.toInt()
    const val LISTEN_STROKE = 0xFFF4F1EA.toInt()
    const val ICON = 0xFFF4F1EA.toInt()
    const val LABEL = 0xFFF4F1EA.toInt()
    const val CANCEL = 0xFFE8E4DC.toInt()
    const val DONE = 0xFFF4F1EA.toInt()

    /** Soft pulse tint (still monochrome). */
    const val PULSE = 0x33F4F1EA.toInt()

    const val ROUND_HARD = "hard"
    const val ROUND_SOFT = "soft"
    const val ROUND_ROUND = "round"

    fun normalizeRoundness(value: String): String = when (value.lowercase()) {
        ROUND_SOFT, ROUND_ROUND -> value.lowercase()
        else -> ROUND_HARD
    }

    fun pctFromLegacy(roundness: String): Int {
        val n = roundness.trim().lowercase()
        return when (n) {
            ROUND_HARD -> 0
            ROUND_SOFT -> 50
            ROUND_ROUND -> 100
            else -> n.toIntOrNull()?.coerceIn(0, 100) ?: 50
        }
    }

    fun cornerPx(
        shape: String,
        density: Float,
        roundness: String = ROUND_HARD,
    ): Float = cornerPx(shape, density, pctFromLegacy(roundness))

    fun cornerPx(shape: String, density: Float, pct: Int): Float {
        val t = pct.coerceIn(0, 100) / 100f
        return when (shape) {
            "circle", "dot" -> 999f * density
            "pill" -> (12f + (24f - 12f) * t) * density
            "listen", "square" -> (2f + (16f - 2f) * t) * density
            else -> (2f + (16f - 2f) * t) * density
        }
    }

    fun strokePx(density: Float): Int =
        (2f * density).toInt().coerceAtLeast(2)
}
