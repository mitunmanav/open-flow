package dev.openflow.dictation.core.stt

/**
 * What [SpeechProvider.prepare] did, or why it could not.
 *
 * A result rather than an exception because the common V1 outcome is *not* a
 * failure of the app: ADR-0010's Model Delivery makes the streaming ASR model
 * a first-launch download, so on a fresh install the model is simply not there
 * yet and that has to be a reportable state — `MODEL_MISSING` from
 * [SpeechProvider.health], surfaced as a download prompt — rather than a crash
 * on the path to first dictation. Throwing would make the one expected
 * first-launch condition look like a bug and push every caller toward a
 * `try`/`catch` that cannot tell this apart from a real one.
 *
 * Reuses [FailureReason] rather than introducing a second error vocabulary.
 * Two enums describing why something failed would be the same
 * two-sources-of-truth defect that replaced `estimatedCostAvailability`, and
 * the router already has to switch on [FailureReason] to decide fallback — a
 * caller forced to translate between two spellings of the same condition
 * eventually drops one.
 */
sealed interface PrepareResult {

    /** The engine is loaded and warmed; [ProviderState.READY]. */
    data object Ready : PrepareResult

    /**
     * Preparation did not complete, and [ProviderState.NOT_PREPARED] stands.
     *
     * [reason] is a [FailureReason], so a router can key its fallback off it
     * exactly as it would a mid-call [SpeechEvent.Failure]. Not every value
     * applies before any audio exists: [FailureReason.OfflineModelMissing],
     * [FailureReason.Timeout] and [FailureReason.Busy] are the realistic ones,
     * and [FailureReason.UnsupportedLanguage] cannot happen at all, because
     * language is chosen per request rather than at load time.
     *
     * [detail] is a human-readable explanation for a log, never for a decision.
     * No code should branch on it, and nothing renders it to the user — the
     * user-facing remedies are decided by the router from [reason] and
     * [SpeechProvider.health], because they are three fixed answers rather than
     * one message per adapter.
     */
    data class Failed(
        val reason: FailureReason,
        val detail: String? = null,
    ) : PrepareResult
}