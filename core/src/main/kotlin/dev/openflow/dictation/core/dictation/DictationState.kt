package dev.openflow.dictation.core.dictation

/**
 * The states of the Dictation lifecycle (ADR-0002).
 *
 * ### Why these live in `core` and not in `app`
 *
 * Settled as part of ticket 42, which asked the question explicitly and
 * required the answer not be left ambiguous. ADR-0005 lists `dictation/` among
 * `core`'s packages and describes `core` as holding "contracts and state
 * machines"; this is the only state machine ADR-0002 defines, so the rule
 * covers it by name rather than by analogy. Three further reasons, in order of
 * how much they would have hurt if answered the other way:
 *
 * 1. ADR-0005 puts core's "FakeProvider pipeline, transition table" unit tests
 *    in `core`. A transition-table test that cannot name the states it drives
 *    cannot live where ADR-0005 says it lives, so putting the states in `app`
 *    would have meant either breaking that rule or writing `core`'s own test
 *    suite against a type it cannot see.
 * 2. ADR-0002 says the Bubble renders purely from `DictationState` payload, and
 *    ADR-0005's dependency arrow runs `app ──► core`. States in `app` would
 *    still render, but only because `app` is the sole consumer — the moment a
 *    second reader exists (History, a service, an instrumented test harness in
 *    a provider module) the arrow points the wrong way for it.
 * 3. The provider half of a Dictation participates without being in `app` at
 *    all: a [dev.openflow.dictation.core.stt.SpeechProvider] fails
 *    mid-utterance and the pipeline has to reach `ErrorRecoverable` from that.
 *    With the states in `app`, the mapping from `SpeechEvent.Failure` to a
 *    Dictation state would have to live in the controller in `app`, and
 *    ADR-0001's `MODEL_MISSING`/`OfflineModelMissing` pairing could not be
 *    asserted anywhere below the UI.
 *
 * So: `core/dictation`, with the contract types in `core/stt` beside it, as
 * ADR-0005's package list has them.
 *
 * ### What is deliberately absent
 *
 * The transitions. ADR-0002 fixes the states, the rules, and that every
 * (state, event) pair has exactly one defined transition or is explicitly
 * ignored — but no ticket owns writing the table, and inventing one here would
 * be a decision ADR-0002 does not contain. [DictationEventChannel] therefore
 * carries no event type either: ADR-0002 names the states and the single-writer
 * rule, never an event, so the event vocabulary is the controller's to settle.
 */
sealed interface DictationState {

    /**
     * No Dictation in flight. The idle Bubble's state, and where every
     * auto-returning terminal state lands.
     */
    data object Idle : DictationState

    /**
     * Choosing a provider and loading it. Where a Dictation begins, and where
     * the controller reads a health snapshot to decide what happens next
     * (ADR-0004 rule 5).
     */
    data object Preparing : DictationState

    /**
     * Capturing audio. [startedAtMs] is the utterance's start, which is what
     * Final Latency is measured against later — an observation about the
     * microphone, so a provider cannot supply it.
     *
     * A streaming provider's partials overlap this state; ADR-0002 is explicit
     * that `Transcribing` is not where live text comes from.
     */
    data class Recording(val startedAtMs: Long) : DictationState

    /**
     * Recording stopped, the provider is finalizing. May be brief or
     * invisible: a provider that emits its `Final` at the endpoint can be
     * `Transcribing` and `Refining` in the same instant, and that is not a
     * state to be prevented — only a state to be well defined.
     */
    data object Transcribing : DictationState

    /**
     * [rawText] is the provider's `Final`, before any refinement. Carried so
     * the Bubble can show what was heard while the refiner works, and so
     * ADR-0002's "a refiner failure degrades to the raw transcript marked
     * unprocessed" has something to degrade *to*.
     */
    data class Refining(val rawText: String) : DictationState

    /**
     * Writing [refinedText] at the frozen Target Snapshot.
     *
     * Non-cancellable: insertion either lands or fails, and the Bubble reflects
     * which. A Dictation cancelled here must not abandon a write already in
     * flight, because the user's text would be in the clipboard and nowhere
     * else with no record of it.
     *
     * [targetDescription] is a human-readable rendering of the snapshot, not the
     * snapshot itself: the Bubble shows "into Notes" and nothing above it needs
     * the insertion characteristics.
     */
    data class Inserting(
        val refinedText: String,
        val targetDescription: String,
    ) : DictationState

    /**
     * Terminal, and auto-returns to [Idle]. [transcriptId] identifies what was
     * persisted and [outcome] how its text reached the user.
     */
    data class Done(
        val transcriptId: TranscriptId,
        val outcome: DictationOutcome,
    ) : DictationState

    /**
     * Terminal, and auto-returns to [Idle]. Audio discarded and nothing
     * persisted — which is why there is no payload: there is deliberately no
     * transcript to offer, and a state that could carry one would eventually
     * carry one by accident.
     */
    data object Cancelled : DictationState

    /**
     * Terminal, and auto-returns to [Idle]. The Bubble shows a problem state and
     * offers retry or copy; the transcript is preserved if one exists.
     *
     * [transcriptId] is null exactly when no transcript does — a microphone that
     * never opened, a router that chose nobody. That distinction is what lets the
     * Bubble offer "copy" without offering copy of nothing.
     */
    data class ErrorRecoverable(
        val reason: DictationError,
        val transcriptId: TranscriptId?,
    ) : DictationState

    /**
     * Terminal, and **not** auto-returning: the next Dictation re-enters
     * [Preparing]. A microphone permission revoked or a model that will not
     * load leaves the app unable to dictate at all, and a Bubble that cleared
     * itself back to idle would hide that until the user tried again and
     * failed again.
     */
    data class ErrorFatal(val reason: DictationError) : DictationState
}

/**
 * Whether this state ends the Dictation: one of [DictationState.Done],
 * [DictationState.Cancelled], [DictationState.ErrorRecoverable] or
 * [DictationState.ErrorFatal].
 *
 * A Dictation has exactly one of these as its outcome (see `GLOSSARY.md`), so
 * History reads terminal states and nothing else — and "is this terminal" is
 * the question a consumer asks first, which is why it is a predicate here
 * rather than something each of them matches on.
 */
val DictationState.isTerminal: Boolean
    get() = when (this) {
        is DictationState.Done,
        is DictationState.Cancelled,
        is DictationState.ErrorRecoverable,
        is DictationState.ErrorFatal,
        -> true

        else -> false
    }

/**
 * Whether the controller returns to [DictationState.Idle] by itself after
 * showing this state.
 *
 * True for three of the four terminal states and false for
 * [DictationState.ErrorFatal]. The distinction is worth a predicate of its own
 * because it is exactly the mistake a reader makes once: treating "terminal" as
 * "clears itself" and quietly dropping a fatal error off the Bubble, so the
 * user is told nothing and only finds out on the next attempt.
 */
val DictationState.autoReturnsToIdle: Boolean
    get() = isTerminal && this !is DictationState.ErrorFatal