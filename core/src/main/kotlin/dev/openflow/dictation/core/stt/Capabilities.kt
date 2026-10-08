package dev.openflow.dictation.core.stt

/**
 * Everything the rest of OpenFlow believes about a speech engine without
 * checking it again.
 *
 * This is the whole of provider neutrality. There is no `if provider == X`
 * anywhere in the app, so every behavioural difference between engines is a
 * field here: the router selects on these, the Bubble builds its toggles from
 * these, the cost ceiling is computed from these. A behaviour the app needs
 * that no field expresses is a missing field, not a place to branch.
 *
 * ### Every field is required, with no defaults
 *
 * Not an oversight. A default makes an undeclared capability look declared,
 * and a declaration the rest of the app believes is exactly the thing
 * [Capabilities Honesty] is about — "omitting it is a correct answer, and
 * declaring it is a false promise the rest of the app believes". Writing
 * `false` is a decision; getting it from a default parameter is an accident
 * that looks identical in review.
 *
 * @param streaming Emits [SpeechEvent.Partial] before [SpeechEvent.Final].
 *   A one-shot engine that decodes a complete waveform and returns one result
 *   must declare `false` here rather than fabricate a partial from a completed
 *   decode.
 * @param offline Runs with no network. A binding declaration, not a
 *   preference: an engine whose offline mode still resolves a token or checks
 *   a licence on first use is lying, and the Contract Test runs the offline
 *   cases with no network available to catch it.
 * @param supportedLanguages Language tags this instance can actually serve,
 *   compared exactly. Declaring `en-US` and quietly failing `en-GB` is the
 *   common real shape of this bug, so the Contract Test checks the whole set
 *   rather than a sample.
 * @param autoDetectLanguage The provider can identify the language without
 *   being told it in the request.
 * @param partialTranscripts [SpeechEvent.Partial] is emitted at all. Redundant
 *   with [streaming] on purpose: the app offers a live-transcript surface from
 *   one and the Contract Test asserts behaviour from the other, so a change to
 *   either alone is a visible change rather than a silent drift.
 * @param timestamps [SpeechEvent.Final]'s `startedAtMs`/`endedAtMs` are
 *   measured, not synthesised. Final Latency is derived from them, so this is
 *   a declaration about measurement quality rather than about a feature.
 * @param confidence Confidence scores are produced. Note that sherpa-onnx has
 *   no word-level confidence, so the V1 provider declares `false` and the
 *   router treats confidence as not-applicable rather than as zero.
 * @param vocabularyBias A biasing vocabulary is accepted per request.
 * @param pricing The Declared Rate, or `null` when the provider cannot
 *   estimate. `null` is not `Pricing.FREE`; see [Pricing].
 * @param maxAudioDurationSeconds This provider's own ceiling, which the
 *   controller enforces and the cost ceiling is computed against. A real
 *   ceiling: the controller stops capture at it.
 */
data class Capabilities(
    val streaming: Boolean,
    val offline: Boolean,
    val supportedLanguages: Set<String>,
    val autoDetectLanguage: Boolean,
    val partialTranscripts: Boolean,
    val timestamps: Boolean,
    val confidence: Boolean,
    val vocabularyBias: Boolean,
    val pricing: Pricing?,
    val maxAudioDurationSeconds: Int,
) {
    init {
        require(supportedLanguages.isNotEmpty()) {
            "supportedLanguages must not be empty: a provider that can serve no " +
                "language can serve no request, so this is a construction bug rather " +
                "than a runtime condition to discover while the user is waiting."
        }
        require(maxAudioDurationSeconds > 0) {
            "maxAudioDurationSeconds must be positive, was $maxAudioDurationSeconds. " +
                "The controller enforces it as a real ceiling, so a non-positive " +
                "value would refuse every utterance."
        }
    }

    /** Whether this provider declares it can serve [language], matched exactly. */
    fun supports(language: String): Boolean = language in supportedLanguages
}