package app.openflow.ui.theme

/**
 * Bubble overlay fill presets. Packed ARGB ints — JVM-safe, no Color.parseColor.
 * Each tint has fill + on-text that stay readable on light and dark hosts.
 */
object BubbleTint {
    const val CHARCOAL = "charcoal"
    const val CREAM = "cream"
    const val INK = "ink"
    const val STONE = "stone"
    const val SKY = "sky"
    const val FOREST = "forest"
    const val CORAL = "coral"
    const val GRAPE = "grape"

    data class Spec(val id: String, val label: String, val fillArgb: Int, val onArgb: Int)

    val ALL: List<Spec> = listOf(
        Spec(CHARCOAL, "Charcoal", 0xFF1A1A18.toInt(), 0xFFF4EFE6.toInt()),
        Spec(CREAM, "Cream", 0xFFF4EFE6.toInt(), 0xFF1A1A18.toInt()),
        Spec(INK, "Ink", 0xFF3D5A80.toInt(), 0xFFF4EFE6.toInt()),
        Spec(STONE, "Stone", 0xFFE8E4DC.toInt(), 0xFF1A1A18.toInt()),
        Spec(SKY, "Sky", 0xFFB8D4E8.toInt(), 0xFF1A1A18.toInt()),
        Spec(FOREST, "Forest", 0xFF2F4F3E.toInt(), 0xFFF4EFE6.toInt()),
        Spec(CORAL, "Coral", 0xFFE8A090.toInt(), 0xFF1A1A18.toInt()),
        Spec(GRAPE, "Grape", 0xFF4A3F5C.toInt(), 0xFFF4EFE6.toInt()),
    )

    fun normalize(id: String): String {
        val n = id.lowercase()
        return ALL.find { it.id == n }?.id ?: CHARCOAL
    }

    fun spec(id: String): Spec = ALL.find { it.id == normalize(id) } ?: ALL.first()

    /** Android packed ARGB fill. */
    fun argb(id: String): Int = spec(id).fillArgb

    /** Contrast ink/cream on fill so light faces stay readable. */
    fun onArgb(id: String): Int = spec(id).onArgb

    /**
     * Stage behind live bubble preview — opposite of light/dark fill so cream
     * never vanishes on cream page bg.
     */
    fun previewStageArgb(id: String): Int {
        val fill = argb(id)
        val luminance = ((fill shr 16) and 0xFF) * 0.299 +
            ((fill shr 8) and 0xFF) * 0.587 +
            (fill and 0xFF) * 0.114
        return if (luminance >= 140.0) argb(CHARCOAL) else argb(CREAM)
    }

    /** Soft pulse from on-color (works for light + dark tints). */
    fun pulseArgb(onArgb: Int, alpha: Int = 0x33): Int =
        (alpha.coerceIn(0, 0xFF) shl 24) or (onArgb and 0x00FFFFFF)
}
