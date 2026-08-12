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

enum class CleanupLevel {
    /** Minimal / raw STT only. */
    RAW,
    /** Fillers + light punct; no self-correct. */
    LIGHT,
    /** Fillers, reps, false starts, self-correct, punct. */
    NORMAL,
    /** Same local rules as NORMAL for now; room for aggressive later. */
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
