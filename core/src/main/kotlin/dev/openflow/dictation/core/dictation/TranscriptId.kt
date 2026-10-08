package dev.openflow.dictation.core.dictation

/**
 * Identifies one persisted transcript.
 *
 * A value class rather than a bare `String` because ADR-0002 passes this
 * between the state machine and History — `Done(transcriptId, outcome)` and
 * `ErrorRecoverable(reason, transcriptId?)` — and the nullable half of that
 * pair is the interesting part: a Dictation that failed before any words were
 * captured has no transcript at all, and that is a different situation from
 * one whose transcript exists but whose insertion failed. A bare `String`
 * would have made the second case expressible as `""`, and an empty id is not
 * a transcript.
 *
 * [value] is whatever the persistence layer uses; this type deliberately does
 * not define an id format, because History Storage owns that and a format
 * baked into the state machine would be a second source of truth for it.
 */
@JvmInline
value class TranscriptId(val value: String) {
    init {
        require(value.isNotBlank()) { "a TranscriptId must identify something" }
    }
}