package dev.openflow.dictation.core.dictation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The classification of ADR-0002's states, and the payload shapes the Bubble and
 * History read.
 *
 * The terminal / auto-return split is the part worth a test rather than a
 * comment. "Terminal" and "clears itself" are different properties, three of the
 * four terminal states have both, and `ErrorFatal` has only the first — so a
 * reader who conflates them drops a fatal error off the Bubble and the user
 * finds out by pressing the button again.
 */
class DictationStateTest {

    @Test
    fun theTerminalStatesAreTheFourADROneSettles() {
        val terminal = listOf(
            DictationState.Done(TranscriptId("t1"), DictationOutcome.INSERTED),
            DictationState.Cancelled,
            DictationState.ErrorRecoverable(DictationError.Timeout, null),
            DictationState.ErrorFatal(DictationError.MicrophoneUnavailable),
        )

        terminal.forEach { state ->
            assertTrue("$state must be terminal", state.isTerminal)
        }
        assertEquals(
            "GLOSSARY.md: a Dictation has exactly one terminal outcome among " +
                "Done, Cancelled, ErrorRecoverable, ErrorFatal. A fifth would mean " +
                "History's read of terminal outcomes needs a case nobody wrote.",
            4,
            terminal.size,
        )
    }

    @Test
    fun theInProgressStatesAreNotTerminal() {
        val inProgress = listOf(
            DictationState.Idle,
            DictationState.Preparing,
            DictationState.Recording(0L),
            DictationState.Transcribing,
            DictationState.Refining("raw"),
            DictationState.Inserting("refined", "into Notes"),
        )

        inProgress.forEach { state ->
            assertFalse("$state is still running", state.isTerminal)
            assertFalse("$state must not auto-return", state.autoReturnsToIdle)
        }
    }

    @Test
    fun errorFatalIsTerminalButDoesNotClearItself() {
        val fatal = DictationState.ErrorFatal(DictationError.MicrophoneUnavailable)

        assertTrue(fatal.isTerminal)
        assertFalse(
            "a fatal error stays on the Bubble. ADR-0002: the next Dictation " +
                "re-enters Preparing, and a Bubble that had quietly returned to " +
                "Idle would hide an app that cannot dictate until the user tried " +
                "again and failed again.",
            fatal.autoReturnsToIdle,
        )
    }

    @Test
    fun theOtherThreeTerminalStatesAutoReturnToIdle() {
        val autoReturning = listOf(
            DictationState.Done(TranscriptId("t1"), DictationOutcome.COPIED),
            DictationState.Cancelled,
            DictationState.ErrorRecoverable(DictationError.InsertFailed, TranscriptId("t2")),
        )

        autoReturning.forEach { state ->
            assertTrue(
                "$state returns to Idle by itself once its outcome is recorded",
                state.autoReturnsToIdle,
            )
        }
    }

    @Test
    fun recoverableFailureCarriesATranscriptOnlyWhenOneExists() {
        // ADR-0002's `transcriptId?` is the distinction the Bubble's retry/copy
        // affordance is built on: a Dictation that failed before any words were
        // captured has nothing to copy, and offering "copy" for it would offer
        // the user an empty string and a button.
        val withoutTranscript = DictationState.ErrorRecoverable(DictationError.NoProviderAvailable, null)
        val withTranscript = DictationState.ErrorRecoverable(
            DictationError.InsertFailed,
            TranscriptId("t3"),
        )

        assertEquals(null, withoutTranscript.transcriptId)
        assertEquals(TranscriptId("t3"), withTranscript.transcriptId)
    }

    @Test
    fun cancelledCarriesNoPayload() {
        // Deliberately payload-free: a cancelled Dictation discarded its audio and
        // persisted nothing, so there is no transcript to offer. A payload here
        // would eventually be populated by an accident, and the privacy policy
        // says a cancelled Dictation leaves nothing behind.
        val cancelled = DictationState.Cancelled
        assertTrue(cancelled.isTerminal)
        assertTrue(cancelled.autoReturnsToIdle)
        assertEquals("Cancelled", cancelled::class.simpleName)
    }

    @Test
    fun recordingCarriesTheUtteranceStartThatFinalLatencyIsMeasuredAgainst() {
        val recording = DictationState.Recording(startedAtMs = 1_700_000_000_000L)

        assertEquals(1_700_000_000_000L, recording.startedAtMs)
    }

    @Test
    fun aBlankTranscriptIdIsRejected() {
        // A `String` would have made an empty id a way to say "there is no
        // transcript", which is what the nullable field on ErrorRecoverable is
        // for -- and an id is an identity, not an absence.
        val thrown = try {
            TranscriptId("  ")
            null
        } catch (expected: IllegalArgumentException) {
            expected.message
        }
        assertTrue("expected a blank id to be rejected, got $thrown", thrown != null)
    }

    @Test
    fun theStatesAreTheOnesADROneLists() {
        // ADR-0002 names ten, and the Bubble renders purely from this payload, so
        // a rename or an addition changes UI that does not exist yet rather than
        // code that does. Pinning the set here means the change has to be made
        // against the ADR.
        val declared = DictationState::class.java.declaredClasses
            .map { it.simpleName }
            .sorted()

        assertEquals(
            listOf(
                "Cancelled",
                "Done",
                "ErrorFatal",
                "ErrorRecoverable",
                "Idle",
                "Inserting",
                "Preparing",
                "Recording",
                "Refining",
                "Transcribing",
            ),
            declared,
        )
    }

    @Test
    fun dictationErrorDoesNotReuseTheProviderFailureVocabulary() {
        // A Dictation can fail at the microphone, at insertion, or with no
        // provider chosen at all, and none of those is something a speech engine
        // reported. Keeping DictationError a separate enum makes "the engine
        // failed" and "the Dictation failed" a type boundary rather than a
        // judgement call at each call site -- and it keeps core.dictation from
        // depending on core.stt, which is the same reason ADR-0005's packages are
        // separate.
        val dictationReasons = DictationError.entries.map { it.name }.toSet()
        val providerReasons = dev.openflow.dictation.core.stt.FailureReason.entries
            .map { it.name }
            .toSet()

        assertTrue(
            "Timeout is the one value both vocabularies would plausibly name, and " +
                "having it in both is how a caller ends up unable to say which " +
                "layer timed out. Found $dictationReasons",
            dictationReasons.intersect(providerReasons).isEmpty(),
        )
        assertTrue("Timeout must exist on the Dictation side", "Timeout" in dictationReasons)
    }

    @Test
    fun theOutcomesAreTheThreeInsertionDispositionsADROneDescribes() {
        // ADR-0002 names `outcome` without defining it, so the values come from
        // ADR-0003's insertion layering: the text arrived, or it reached the
        // clipboard and the user was prompted, or the user declined. Insertion
        // failure is deliberately not an outcome -- it is ErrorRecoverable,
        // because a Dictation whose text was never delivered has not finished.
        assertEquals(
            listOf("INSERTED", "COPIED", "DECLINED"),
            DictationOutcome.entries.map { it.name },
        )
    }
}