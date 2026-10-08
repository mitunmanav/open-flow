package dev.openflow.dictation.core.stt

/**
 * The lifecycle of a [SpeechProvider] instance: what the **app** has done to
 * it.
 *
 * Four values, and none of them shared with [ProviderHealth]. `READY` here
 * says the app finished preparing this instance; it says nothing about
 * whether the engine can serve a dictation, which is what
 * [SpeechProvider.health] is for. An instance can be `READY` and report
 * `UNAVAILABLE`, and a `CLOSED` instance has no health verdict at all because
 * closing is something the app did rather than something an engine reported.
 *
 * See [ProviderHealth] for the split this type exists to protect, and
 * [isConsistentWith] for the one relationship between the two vocabularies
 * that is declared rather than forbidden.
 */
enum class ProviderState {
    /** Created; nothing loaded. */
    NOT_PREPARED,

    /** [SpeechProvider.prepare] is in flight. */
    PREPARING,

    /** Loaded and warmed. Not necessarily able to serve — see [ProviderHealth]. */
    READY,

    /** Released by [SpeechProvider.close]. Terminal: the instance is spent. */
    CLOSED,
}