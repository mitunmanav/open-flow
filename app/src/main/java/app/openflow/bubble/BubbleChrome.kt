package app.openflow.bubble

/**
 * Minimal brutal chrome for the Flow Bubble overlay.
 * Cream / charcoal, hard edges, no soft Material zinc/indigo.
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

    /** Hard-edge corner in px. Listen bar always hard. */
    fun cornerPx(shape: String, density: Float): Float = when (shape) {
        "circle", "dot" -> 999f * density
        "pill" -> 12f * density
        "listen", "square" -> 2f * density
        else -> 2f * density
    }

    fun strokePx(density: Float): Int =
        (2f * density).toInt().coerceAtLeast(2)
}
