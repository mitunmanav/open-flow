package app.openflow.text

/**
 * Features light up from ear + brain pick. No extra toggles.
 * String ids only — do not import engine/.
 */
object FeatureAuto {

    private val CLOUD_EARS = setOf(
        "openai", "deepgram", "assemblyai", "sarvam", "custom_stt",
    )
    private val RULES_BRAINS = setOf("none", "on_phone")

    fun earNeedsNet(earId: String): Boolean =
        earId.trim().lowercase() in CLOUD_EARS

    fun of(
        earId: String,
        brainId: String,
        languages: Set<String> = emptySet(),
    ): Set<Feature> {
        val ear = earId.trim().lowercase()
        val brain = brainId.trim().lowercase()
        val cloud = ear in CLOUD_EARS
        val rewrite = brain.isNotEmpty() && brain !in RULES_BRAINS
        return Feature.entries.filter { feature ->
            FeatureGate.can(
                feature,
                brainRewrite = rewrite,
                brainCommand = rewrite,
                streamLive = cloud,
                languages = languages,
                earNeedsNet = cloud,
                earId = ear,
            )
        }.toSet()
    }
}
