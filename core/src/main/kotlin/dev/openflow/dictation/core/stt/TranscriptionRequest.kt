package dev.openflow.dictation.core.stt

/**
 * One dictation's worth of transcription, as the Adaptive Dictation Router
 * hands it to the provider it selected.
 *
 * Deliberately free of app identity. The Dictation's Target Snapshot and the
 * frozen per-app settings belong to the Dictation Controller (ADR-0002), and
 * ADR-0002 says so explicitly — "RouterContext and speech-provider requests
 * need no app identity". Keeping them out is what lets a provider be written
 * without importing anything about insertion, and it is why a provider cannot
 * grow a per-app behaviour branch.
 *
 * @param language The BCP-47 tag to transcribe as, e.g. `"en-US"`. Per
 *   request, not per instance: one provider instance serves one model
 *   configuration, and swapping models means a new instance (ADR-0001). A
 *   plain `String` rather than a `LanguageCode` wrapper on purpose — the
 *   contract does not get to define a tag's normalization rules, and a
 *   wrapper would have to either adopt a normalization the engines do not
 *   share or silently accept whatever it was handed. Comparison is exact, so
 *   an engine that serves `en` but not `en-US` has to declare exactly that.
 * @param requireOffline This dictation must not touch the network. Derived by
 *   the router from `RouterContext.offline`; a provider whose
 *   [Capabilities.offline] is true ignores it because it has no network path
 *   to take, and the router has already excluded the others.
 * @param vocabularyBias Terms to bias recognition towards, for providers whose
 *   [Capabilities.vocabularyBias] is true. Empty otherwise, and ignored
 *   entirely rather than failed: the router filters on the capability before
 *   it gets here, so a non-empty list on a provider that declared no bias is a
 *   router bug this type does not need to catch.
 */
data class TranscriptionRequest(
    val language: String,
    val requireOffline: Boolean = false,
    val vocabularyBias: List<String> = emptyList(),
) {
    init {
        require(language.isNotBlank()) {
            "language must not be blank: a request with no language is not a " +
                "request any provider can serve, and would otherwise reach the " +
                "engine as a silent default."
        }
    }
}