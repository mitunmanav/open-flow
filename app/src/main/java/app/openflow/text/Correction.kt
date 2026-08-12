package app.openflow.text

/**
 * Explicit self-correction analysis (not "LLM make better").
 * Example: 430 → actually → 530
 */
data class Correction(
    val originalText: String,
    val replacementText: String,
    val marker: String,
    val confidence: Float = 0.8f
)

/**
 * Pref keys: none | light | medium | high (Wispr Auto Cleanup names).
 * LOCAL stages only — no cloud rewrite.
 */
enum class CleanupLevel {
    /** None: exact STT. */
    RAW,
    /** Light: normalize, fillers, reps, commands, light grammar. */
    LIGHT,
    /** Medium: + false starts, course-correct, lists, light clarity. */
    NORMAL,
    /** High: + short hedge / wordiness rules. */
    HIGH;

    companion object {
        fun fromPref(value: String): CleanupLevel = when (value.lowercase()) {
            "none", "raw" -> RAW
            "light" -> LIGHT
            "high" -> HIGH
            else -> NORMAL // medium / normal
        }
    }
}

/**
 * Full pipeline result — raw always kept.
 */
data class CleanupResult(
    val raw: String,
    val clean: String,
    val corrections: List<Correction> = emptyList(),
    val level: CleanupLevel = CleanupLevel.NORMAL
)
