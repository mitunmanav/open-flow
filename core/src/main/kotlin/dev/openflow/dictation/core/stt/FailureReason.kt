package dev.openflow.dictation.core.stt

/**
 * Why a [SpeechProvider] could not produce a transcript.
 *
 * Typed, not a message string, because the Adaptive Dictation Router keys its
 * fallback off this enum: a candidate is dropped from the chain when it
 * reports `reason` with `recoverable` (ADR-0004). A stringly-typed failure
 * would make that a substring match, and the one string that must never be
 * guessed at is the difference between "this engine cannot help" and "try it
 * again".
 */
enum class FailureReason {

    /**
     * The model is not on disk, or is present but unloadable.
     *
     * Distinct from [AudioCaptureFailed] and from health's
     * `MODEL_MISSING` only in where it is observed: health reports the
     * pre-call verdict, this the mid-call outcome. The pairing is an
     * invariant the Contract Test enforces — a provider reporting
     * `MODEL_MISSING` from `health()` must fail `transcribe()` with this and
     * not something generic, because that pairing is what lets the router show
     * a download prompt rather than a retry button.
     *
     * `recoverable = true`: another provider can serve the request.
     */
    OfflineModelMissing,

    /**
     * The request named a language this provider does not serve. Always
     * reported with `recoverable = false`, because a router that had the
     * language filter in ADR-0004 rule 3 would not have asked — so a provider
     * that raises this for a language it declares has contradicted its own
     * [Capabilities], which is a Capabilities Honesty failure rather than a
     * runtime condition.
     */
    UnsupportedLanguage,

    /**
     * The microphone could not be opened or stopped: permission denied or
     * revoked, the device in use by another app, the input device gone.
     * Retrying unchanged will not help while the cause holds.
     */
    AudioCaptureFailed,

    /**
     * A suspending step exceeded its budget. ADR-0002's rule is that every
     * suspending step has a timeout, so this is the normal report of a hang
     * rather than an exceptional one.
     */
    Timeout,

    /**
     * The engine is already transcribing and will not start a second request.
     * Recoverable in the ADR-0001 sense — the Dictation can be retried on the
     * same provider once the current utterance ends — which is exactly why it
     * is not folded into [Timeout]: the remedy differs.
     */
    Busy,

    /**
     * The Dictation was cancelled, so transcription was stopped deliberately.
     *
     * Not an error condition and not a fallback trigger: ADR-0004 excludes
     * `Cancelled` from fallback precisely because the user asked for this. A
     * provider should generally end the flow without emitting this at all —
     * cancellation propagates by cancelling the collecting coroutine — but the
     * value exists because a one-shot engine that cannot be interrupted mid
     * decode has to report it somewhere.
     */
    Cancelled,

    /**
     * Last resort, and the only value here that is allowed to be unhelpful.
     *
     * Prefer a specific reason: [UnsupportedLanguage] removes a candidate the
     * Dictation would otherwise have been fine with, whereas [Unknown] removes
     * every candidate from the fallback chain with nothing to say why. There is
     * no provider in V1 for which the difference is academic.
     */
    Unknown,
}