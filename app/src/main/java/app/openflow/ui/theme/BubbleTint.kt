package app.openflow.ui.theme

/**
 * Bubble overlay fill presets. Packed ARGB ints — JVM-safe, no Color.parseColor.
 */
object BubbleTint {
    const val CHARCOAL = "charcoal"
    const val CREAM = "cream"
    const val INK = "ink"
    const val STONE = "stone"

    fun normalize(id: String): String = when (id.lowercase()) {
        CREAM, INK, STONE, CHARCOAL -> id.lowercase()
        else -> CHARCOAL
    }

    /** Android packed ARGB. */
    fun argb(id: String): Int = when (normalize(id)) {
        CREAM -> 0xFFF4EFE6.toInt()
        INK -> 0xFF3D5A80.toInt()
        STONE -> 0xFFE8E4DC.toInt()
        else -> 0xFF1A1A18.toInt()
    }

    /** Contrast ink/cream on [argb] so cream/stone faces stay readable. */
    fun onArgb(id: String): Int = when (normalize(id)) {
        CREAM, STONE -> 0xFF1A1A18.toInt()
        else -> 0xFFF4EFE6.toInt()
    }
}
