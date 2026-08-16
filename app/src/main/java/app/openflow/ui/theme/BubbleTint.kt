package app.openflow.ui.theme

/**
 * Bubble overlay fill presets. Packed ARGB ints — JVM-safe, no Color.parseColor.
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

    val ALL: List<String> = listOf(
        CHARCOAL, CREAM, INK, STONE, SKY, FOREST, CORAL, GRAPE,
    )

    fun normalize(id: String): String = when (id.lowercase()) {
        CREAM, INK, STONE, CHARCOAL, SKY, FOREST, CORAL, GRAPE -> id.lowercase()
        else -> CHARCOAL
    }

    /** Android packed ARGB. */
    fun argb(id: String): Int = when (normalize(id)) {
        CREAM -> 0xFFF4EFE6.toInt()
        INK -> 0xFF3D5A80.toInt()
        STONE -> 0xFFE8E4DC.toInt()
        SKY -> 0xFFB8D4E8.toInt()
        FOREST -> 0xFF2F4F3E.toInt()
        CORAL -> 0xFFE8A090.toInt()
        GRAPE -> 0xFF4A3F5C.toInt()
        else -> 0xFF1A1A18.toInt()
    }

    /** Contrast ink/cream on [argb] so light faces stay readable. */
    fun onArgb(id: String): Int = when (normalize(id)) {
        CREAM, STONE, SKY, CORAL -> 0xFF1A1A18.toInt()
        else -> 0xFFF4EFE6.toInt()
    }

    /**
     * Stage behind live bubble preview — opposite of light/dark fill so cream
     * never vanishes on cream page bg.
     */
    fun previewStageArgb(id: String): Int = when (normalize(id)) {
        CREAM, STONE, SKY, CORAL -> argb(CHARCOAL)
        else -> argb(CREAM)
    }
}
