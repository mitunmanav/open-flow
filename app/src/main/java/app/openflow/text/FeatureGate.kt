package app.openflow.text

enum class Feature {
    HIGH_AI,
    UNDO_AI,
    COMMAND,
    AI_STYLE,
    AI_BACKTRACK,
    FIELD_CONTEXT,
    LIVE_PARTIAL,
    EAR_PUNCT,
    MULTILINGUAL,
    SARVAM_MODE,
}

/**
 * Wispr features light up from ear/brain booleans.
 * Do not import engine/ModelCapability.
 */
object FeatureGate {

    fun can(
        feature: Feature,
        brainRewrite: Boolean = false,
        brainCommand: Boolean = false,
        streamLive: Boolean = false,
        earPunct: Boolean = false,
        languages: Set<String> = emptySet(),
        earNeedsNet: Boolean = false,
        earId: String = "",
    ): Boolean = when (feature) {
        Feature.HIGH_AI,
        Feature.UNDO_AI,
        Feature.AI_STYLE,
        Feature.AI_BACKTRACK,
        Feature.FIELD_CONTEXT -> brainRewrite
        Feature.COMMAND -> brainCommand
        Feature.LIVE_PARTIAL -> streamLive
        Feature.EAR_PUNCT -> earPunct
        Feature.MULTILINGUAL ->
            languages.size > 1 || (languages.isEmpty() && earNeedsNet)
        Feature.SARVAM_MODE -> earId.equals("sarvam", ignoreCase = true)
    }
}
