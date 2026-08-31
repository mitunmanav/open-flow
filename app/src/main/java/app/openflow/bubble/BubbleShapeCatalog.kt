package app.openflow.bubble

/**
 * Single source of truth for floating bubble shapes.
 * Settings chips, prefs normalize, idle size, and corners all read here.
 * Add a row → it shows in UI and paints without hunting string lists.
 */
enum class BubbleShapeSpec(
    val id: String,
    val label: String,
    val idleWidthDp: Float,
    val idleHeightDp: Float,
    val oval: Boolean,
    /** Corner dp at roundPct=0 (ignored when [oval]). */
    val cornerBaseDp: Float,
    /** Corner dp at roundPct=100 (ignored when [oval]). */
    val cornerMaxDp: Float,
) {
    PILL("pill", "Pill", 96f, 48f, oval = false, cornerBaseDp = 12f, cornerMaxDp = 24f),
    SLIM("slim", "Slim", 112f, 36f, oval = false, cornerBaseDp = 14f, cornerMaxDp = 18f),
    CHUNK("chunk", "Chunk", 80f, 56f, oval = false, cornerBaseDp = 10f, cornerMaxDp = 20f),
    STADIUM("stadium", "Stadium", 96f, 48f, oval = false, cornerBaseDp = 24f, cornerMaxDp = 48f),
    CIRCLE("circle", "Circle", 48f, 48f, oval = true, cornerBaseDp = 999f, cornerMaxDp = 999f),
    SQUARE("square", "Squircle", 48f, 48f, oval = false, cornerBaseDp = 2f, cornerMaxDp = 16f),
    DOT("dot", "Dot", 28f, 28f, oval = true, cornerBaseDp = 999f, cornerMaxDp = 999f),
    ;

    fun cornerPx(density: Float, roundPct: Int): Float {
        if (oval) return 999f * density
        val t = roundPct.coerceIn(0, 100) / 100f
        return (cornerBaseDp + (cornerMaxDp - cornerBaseDp) * t) * density
    }

    fun idleSizePx(density: Float): Pair<Int, Int> =
        (idleWidthDp * density).toInt() to (idleHeightDp * density).toInt()
}

object BubbleShapeCatalog {
    val ALL: List<BubbleShapeSpec> = BubbleShapeSpec.entries

    val DEFAULT: BubbleShapeSpec = BubbleShapeSpec.PILL

    fun fromId(raw: String): BubbleShapeSpec {
        val id = raw.trim().lowercase()
        return ALL.find { it.id == id } ?: DEFAULT
    }

    fun normalize(raw: String): String = fromId(raw).id

    fun ids(): List<String> = ALL.map { it.id }
}
