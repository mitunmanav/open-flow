package dev.openflow.dictation.core.dictation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The one channel every component but the controller writes to (ADR-0002:
 * "every other component sends `DictationEvent`s through one channel. No
 * component writes state directly.").
 *
 * That rule is only worth stating if something enforces it, and what enforces
 * it here is a shape rather than a convention: a component is handed *this*,
 * and [events] is the read side, which only the controller takes. There is no
 * signature anywhere in the codebase that a Bubble, an audio source, a
 * provider, a refiner or an inserter could call to move the machine — the
 * `MutableStateFlow<DictationState>` the controller owns is never passed out.
 * ADR-0002's motivation was scattered booleans admitting impossible
 * combinations, and the thing that made those impossible to write was the
 * single writer, not the convention.
 *
 * ### Why the event type is a parameter
 *
 * ADR-0002 settles the states, the rules, and this channel. It does not name a
 * single event — it describes `DictationEvent` as the thing components send
 * without enumerating what any of them are. So this class does not fix one
 * either. Binding it now would mean inventing an event vocabulary ADR-0002 does
 * not contain, and the first controller to be written would have to either live
 * with it or amend an ADR to change it.
 *
 * The constraint that *is* settled holds regardless of what `E` turns out to
 * be: components get a sink, the controller gets a [Flow], and nothing else
 * gets either.
 *
 * @param capacity How many reports may queue before [send] suspends. The
 *   default is one buffer slot per queued report, which is the right shape for
 *   this pipeline: reports are small, the consumer is a coroutine that drains
 *   continuously, and a deep queue would only hide a controller that has stopped
 *   reading. Events are never dropped here — [send] suspends rather than
 *   discarding, because a dropped event is a Dictation that silently skips a
 *   state.
 */
class DictationEventChannel<E : Any>(capacity: Int = Channel.BUFFERED) {

    private val channel = Channel<E>(capacity)

    /**
     * The controller's read side. The single consumer of every report.
     *
     * A cold read over the channel rather than the channel itself, so a caller
     * that merely holds this reference starts nothing — which is the same
     * discipline [dev.openflow.dictation.core.stt.SpeechProvider.transcribe]
     * follows.
     */
    val events: Flow<E> = channel.receiveAsFlow()

    /**
     * Send a report. Suspends while the channel is full.
     *
     * [trySend] is not offered on purpose: `trySend` returns a result rather
     * than waiting, so a caller can discover a full channel and choose to drop
     * the report, and "the controller was busy" is not a reason to skip a
     * state transition.
     */
    suspend fun send(event: E) {
        channel.send(event)
    }

    /**
     * Closes the channel; [events] completes once the queue drains.
     *
     * Reports sent after this throw, so a component that outlives the
     * Dictation finds out rather than writing into a queue nobody reads. That
     * throw *is* the closed signal — no `isOpen` is offered, because a second
     * way to ask invites a caller to poll instead of finding out.
     */
    fun close() {
        channel.close()
    }
}