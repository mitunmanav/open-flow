package dev.openflow.dictation.core.stt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The contract every speech-to-text engine implements (ADR-0001).
 *
 * An internal contract, not a stable API: it may change within the 1.x line,
 * and ADR-0001 revisions plus the CHANGELOG are how a change is announced
 * (ADR-0006). Read `docs/providers/provider-authoring.md` before writing an
 * implementation, and read [capabilities] before writing any of the rest —
 * the declarations there are binding, and the Contract Test suite in `core`'s
 * test fixtures is what makes them binding rather than aspirational.
 *
 * One instance serves one model configuration and language is per request.
 * Swapping models means a new instance held by the router, never a mutating
 * parameter on a live one; that is what lets the router keep several
 * candidates warm at once.
 *
 * ### Where the audio comes from
 *
 * Not a parameter. `transcribe` takes only the request, so an adapter is
 * constructed with whatever it reads audio from — a microphone source, a file,
 * a scripted list of buffers — and `app` wires that in by manual constructor
 * injection (ADR-0005). The contract deliberately does not name an
 * `AudioSource` type, because no such type exists yet in `core`; when it does,
 * naming it here is a change to every adapter, which is the same bar
 * `docs/providers/provider-authoring.md` sets for adding any member here.
 */
interface SpeechProvider {

    /**
     * This adapter's stable identity: the `preferredProviderId` a user can set
     * and the name recorded in the router's decision log (ADR-0004).
     *
     * Must distinguish two adapters over the *same* engine as well as two
     * engines — the router holds several candidates warm at once, and a
     * decision log that cannot say which model answered cannot answer "why did
     * this provider?". A class name is the usual wrong answer here, because two
     * model configurations of one engine share one class.
     */
    val id: String

    /**
     * What this provider can do, declared rather than probed (see the class
     * doc). Available before [prepare]: it describes the model this instance
     * was constructed for, not the state that model is currently in.
     */
    val capabilities: Capabilities

    /**
     * Lifecycle: `NOT_PREPARED` -> `PREPARING` -> `READY` -> `CLOSED`.
     *
     * The only reactive surface in V1, and it reports lifecycle the app itself
     * drives. Deliberately not a health stream — a health stream would invite
     * the automatic health-based rerouting ADR-0004 defers, and V1's only
     * consumer of health is the router reading one snapshot at `PREPARING`.
     */
    val state: StateFlow<ProviderState>

    /**
     * Load the model and engine, moving [state] through `PREPARING` to
     * `READY`, or reporting why it could not.
     *
     * Suspending because loading weights is I/O. Idempotent: calling it on an
     * instance already `READY` returns [PrepareResult.Ready] and loads nothing
     * again, so a router may call it without first asking [state].
     *
     * A [PrepareResult.Failed] leaves the instance at `NOT_PREPARED` — never
     * `READY` — which is what makes the `MODEL_MISSING` pairing consistent:
     * see [isConsistentWith].
     *
     * Throws [IllegalStateException] on an instance already
     * [ProviderState.CLOSED]. Closing is terminal and there is no re-prepare:
     * "swapping models" means a new instance (ADR-0001), so a `prepare()` that
     * resurrected a released one would hide a use-after-release bug rather than
     * surface it.
     */
    suspend fun prepare(): PrepareResult

    /**
     * Transcribe one request, as a **cold** [Flow] of [SpeechEvent].
     *
     * Cold means collecting it twice starts two transcriptions: nothing is
     * captured, decoded or warmed before the first collector arrives, and
     * cancelling the collecting coroutine cancels the transcription and releases
     * the native resources it held. The engine may be used by only one
     * request at a time; a second concurrent request fails with
     * [FailureReason.Busy] rather than queueing, because a queued utterance the
     * user is no longer speaking into is not a transcription.
     *
     * Call this on an instance that is not `READY` only to observe the refusal:
     * the usual path is the router reading [health] and [state] at `PREPARING`
     * and preparing the chosen instance first. A request against a
     * `MODEL_MISSING` instance must fail [FailureReason.OfflineModelMissing]
     * rather than something generic — ADR-0001 makes that pairing an invariant,
     * and the Contract Test enforces it.
     */
    fun transcribe(request: TranscriptionRequest): Flow<SpeechEvent>

    /**
     * The current readiness verdict, read from what this adapter already knows:
     * is the model on disk, is the engine loaded, is the config present, what
     * was the last failure.
     *
     * A plain synchronous method on purpose. It does not probe, never blocks on
     * I/O and never touches the network, because its one V1 consumer is the
     * router reading a single snapshot at `PREPARING` — a health check that
     * blocked would sit on the critical path of every dictation. A verdict that
     * needs a round trip to produce does not belong here.
     *
     * Throws [IllegalStateException] on an instance already
     * [ProviderState.CLOSED]. Refusing is the honest answer: `CLOSED` has no
     * health counterpart because closing is something the app did, so the
     * truthful response to "how ready is this?" about a released engine is
     * none, and returning the last cached verdict would let a released
     * provider look `HEALTHY` to a caller that missed the close.
     */
    fun health(): ProviderHealth

    /**
     * Release native resources — recognizers, streams, threads — and move
     * [state] to `CLOSED`. Idempotent: closing twice is not an error, because
     * the natural time to close is a `finally` block that may run after an
     * explicit close.
     *
     * Terminal. Nothing on a closed instance is callable except [health] and
     * [capabilities], both of which throw or answer as documented above.
     */
    fun close()
}