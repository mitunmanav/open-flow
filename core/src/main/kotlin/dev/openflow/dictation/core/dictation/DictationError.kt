package dev.openflow.dictation.core.dictation

/**
 * Why a Dictation ended in a problem state: the `reason` carried by
 * [DictationState.ErrorRecoverable] and [DictationState.ErrorFatal].
 *
 * Deliberately *not* `FailureReason`. That enum is the speech provider's
 * vocabulary — ADR-0001 gives it to the router so it can key fallback off it —
 * and a Dictation can fail at the microphone, at insertion, or with no provider
 * chosen at all, none of which is a provider's report. Reusing it would both
 * stretch a type whose whole value is that it says what the *engine* said, and
 * make `core.dictation` depend on `core.stt` for no reason. Keeping them apart
 * means the boundary between "the engine failed" and "the Dictation failed" is
 * a type boundary, which is checkable, instead of a judgement call at each
 * call site.
 *
 * Flat, with no payload. The provider's own [reason][FailureReason] does not
 * travel with it, and that is a known gap rather than a settled design: a
 * Dictation that fell over because every candidate provider failed needs the
 * per-candidate reasons to be explainable, and ADR-0004's decision log is
 * where that belongs — not on the state the Bubble renders. Whoever writes the
 * controller should revisit whether History needs it before assuming either
 * way.
 */
enum class DictationError {

    /**
     * A suspending step exceeded its budget. ADR-0002 requires every suspending
     * step to have one and to land here, so this is the normal report of a hang
     * and not an exceptional condition.
     */
    Timeout,

    /**
     * The microphone could not be opened: permission denied or revoked, or the
     * input device in use. Belongs in [DictationState.ErrorFatal] when it will
     * not resolve on its own, because the next Dictation re-enters `Preparing`
     * and cannot get past this.
     */
    MicrophoneUnavailable,

    /**
     * The router chose nobody — no candidate satisfied the request's language
     * and offline requirements — or every candidate was skipped. The user's
     * remedy is in `blockedBy` on the decision, not here: "no speech model
     * installed", "nothing allowed offline" and "over your cost ceiling" are
     * three different fixes, which is the whole reason ADR-0004 returns an
     * empty candidate set as a decision rather than as a null.
     */
    NoProviderAvailable,

    /**
     * A provider was chosen and failed, and the fallback chain ADR-0004 allows
     * was exhausted. Distinct from [NoProviderAvailable] because words were
     * possibly captured before the failure, which decides whether a transcript
     * is preserved and offered for retry.
     */
    TranscribeFailed,

    /**
     * The target could not be written at insertion: the field vanished, is no
     * longer writable, or re-resolution found something else. ADR-0003 keeps the
     * transcript and offers copy, so this is recoverable whenever a transcript
     * exists — which is exactly what the nullable `transcriptId` on
     * [DictationState.ErrorRecoverable] is for.
     */
    InsertFailed,

    /**
     * Last resort. Prefer a specific value for the same reason as
     * [FailureReason.Unknown]: the Bubble's remedy and History's retry affordance
     * are both driven off this, and "something went wrong" supports neither.
     */
    Unknown,
}