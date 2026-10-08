package dev.openflow.dictation.core.stt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * One normalized event from a [SpeechProvider] during one transcription.
 *
 * The sealed set is the point, not the convenience: `when (event)` over these
 * five variants is a total function, so a provider that needs a sixth kind of
 * event has to add it here where the Bubble, History and the router all see
 * it. An open interface would hand every consumer an `else ->` and the
 * guarantee would quietly become "adapters are trusted", which is the defect
 * the whole seam exists to prevent.
 *
 * Ordering within one `transcribe()`: [Preparing] and [Listening] bracket the
 * audio, [Partial] snapshots run between them, and the request terminates on
 * exactly zero or one [Final] or [Failure]. See
 * `docs/providers/provider-authoring.md`.
 */
sealed interface SpeechEvent {

    /**
     * The engine is warming up. Never dropped downstream, because it is what
     * tells the Bubble to show "getting ready" instead of "not listening yet".
     */
    data object Preparing : SpeechEvent

    /**
     * Audio is arriving and being decoded. Not a promise that any recognisable
     * speech has been heard — a provider emits it once the capture path is
     * live, which is what makes it the right moment to show a live indicator.
     */
    data object Listening : SpeechEvent

    /**
     * A **cumulative snapshot** of the utterance so far, not a delta: every
     * [text] is the whole utterance as this provider currently understands it,
     * and each one replaces the last.
     *
     * The distinction is invisible in a test that reads one event and is the
     * difference between a live transcript and garbled text once these are
     * conflated downstream, so the Contract Test asserts it rather than
     * trusting it (ADR-0006: enforcement belongs in the shared suite, not in
     * runtime validation at `prepare()`).
     *
     * Emit these at the engine's natural cadence. Consumers conflate them —
     * [conflatePartials] — so there is nothing to gain from rate-limiting.
     */
    data class Partial(val text: String) : SpeechEvent

    /**
     * The utterance is complete. Utterance-level timestamps in V1, not
     * word-level; whether they are *measured* or synthesised is what
     * [Capabilities.timestamps] declares.
     *
     * [startedAtMs] and [endedAtMs] are expected to satisfy
     * `startedAtMs <= endedAtMs` and to be consistent with the audio actually
     * supplied — but that is deliberately not a constructor `require`. A
     * provider that throws inside its event flow fails the request with an
     * exception instead of a [Failure], which is a worse report than an
     * implausible timestamp; the Contract Test is where the expectation is
     * enforced.
     *
     * An empty [text] is a provider bug rather than a silent utterance: a
     * capture that heard nothing should terminate the request with no terminal
     * event at all, not with an empty transcript the refiner and the
     * inserter would faithfully deliver.
     */
    data class Final(
        val text: String,
        val startedAtMs: Long,
        val endedAtMs: Long,
    ) : SpeechEvent

    /**
     * The request could not produce a transcript.
     *
     * [recoverable] means *the Dictation can still complete by another route* —
     * not that this call will succeed. `FailureReason.OfflineModelMissing` is
     * therefore `true`, because the router falls back to the next eligible
     * provider and the "download the speech model" prompt belongs on screen
     * only once every candidate has failed (ADR-0001). Read it backwards and
     * dictations that could have succeeded are stranded for no reason.
     */
    data class Failure(
        val reason: FailureReason,
        val recoverable: Boolean,
    ) : SpeechEvent
}

/**
 * Keep the latest [SpeechEvent.Partial] and pass everything else through
 * untouched, in order.
 *
 * ADR-0001 asks for exactly this and nothing broader: `Partial` is conflated,
 * and `Preparing`, `Final` and `Failure` are never dropped. The obvious
 * alternative — `flow.conflate()` — breaks the second half, because
 * conflating the whole stream drops whatever is in flight when the consumer
 * is slow, and "whatever is in flight" during a slow consumer is usually the
 * one `Final` the Dictation was waiting on.
 *
 * At most one snapshot is held, and it is emitted *before* the non-`Partial`
 * event that ended its run, so ordering survives: the last snapshot a listener
 * shows still precedes the final transcript that replaces it.
 */
fun Flow<SpeechEvent>.conflatePartials(): Flow<SpeechEvent> = flow {
    var pending: SpeechEvent.Partial? = null
    collect { event ->
        when (event) {
            is SpeechEvent.Partial -> pending = event
            else -> {
                pending?.let { emit(it) }
                pending = null
                emit(event)
            }
        }
    }
    // A request whose flow ended mid-run — cancelled, or a provider that
    // flushed without a terminal event — still owes its consumer the latest
    // snapshot it saw, or the live transcript stops one word short of what the
    // engine actually heard.
    pending?.let { emit(it) }
}