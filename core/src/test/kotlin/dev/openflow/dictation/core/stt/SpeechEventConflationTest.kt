package dev.openflow.dictation.core.stt

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [conflatePartials], which is the only place ADR-0001's conflation rule
 * becomes executable.
 *
 * The rule has two halves and the obvious implementation breaks the second:
 * ADR-0001 says `Partial` is conflated *and* that `Preparing`, `Final` and
 * `Failure` are never dropped, and `Flow.conflate()` conflates the whole stream
 * regardless. When a consumer is slow, "whatever is in flight" is usually the
 * one `Final` the Dictation was waiting on, so a whole-stream conflate drops the
 * event the pipeline exists to deliver. That is worth tests rather than a
 * comment.
 */
class SpeechEventConflationTest {

    private fun conflate(vararg events: SpeechEvent): List<SpeechEvent> = runBlocking {
        events.asFlow().conflatePartials().toList()
    }

    @Test
    fun onlyTheLatestSnapshotOfARunSurvives() {
        val result = conflate(
            SpeechEvent.Preparing,
            SpeechEvent.Listening,
            SpeechEvent.Partial("hello"),
            SpeechEvent.Partial("hello wor"),
            SpeechEvent.Partial("hello world"),
            SpeechEvent.Final("hello world", 0L, 500L),
        )

        assertEquals(
            listOf(
                SpeechEvent.Preparing,
                SpeechEvent.Listening,
                // Three snapshots became one. Each Partial is a cumulative
                // snapshot of the same utterance, so the earlier two carried no
                // information the last one lacks -- except while the consumer is
                // still busy, which is the case conflation exists for.
                SpeechEvent.Partial("hello world"),
                SpeechEvent.Final("hello world", 0L, 500L),
            ),
            result,
        )
    }

    @Test
    fun nonPartialEventsAreNeverDropped() {
        val input = listOf(
            SpeechEvent.Preparing,
            SpeechEvent.Partial("a"),
            SpeechEvent.Final("a", 0L, 100L),
            // A second utterance in the same collection: a provider that serves
            // one request per flow does not produce this, but the operator must
            // not lose an event if a caller concatenates flows.
            SpeechEvent.Listening,
            SpeechEvent.Partial("b"),
            SpeechEvent.Failure(FailureReason.Timeout, recoverable = true),
        )

        assertEquals(
            "Preparing, the Final and the Failure must all survive; only Partials " +
                "are conflatable",
            listOf(
                SpeechEvent.Preparing,
                SpeechEvent.Partial("a"),
                SpeechEvent.Final("a", 0L, 100L),
                SpeechEvent.Listening,
                SpeechEvent.Partial("b"),
                SpeechEvent.Failure(FailureReason.Timeout, recoverable = true),
            ),
            conflate(*input.toTypedArray()),
        )
    }

    @Test
    fun theLastSnapshotStillArrivesBeforeTheEventThatEndedItsRun() {
        // Ordering, not just membership. A conflate implemented as `merge` of a
        // conflated Partial stream and an unfiltered one passes a membership test
        // and loses this: the Final can overtake the snapshot it was meant to
        // replace, and the live transcript then updates after the transcript that
        // superseded it.
        val result = conflate(
            SpeechEvent.Partial("fir"),
            SpeechEvent.Partial("first"),
            SpeechEvent.Final("first", 0L, 200L),
        )

        assertEquals(SpeechEvent.Partial("first"), result[0])
        assertEquals(SpeechEvent.Final("first", 0L, 200L), result[1])
    }

    @Test
    fun aTrailingRunOfSnapshotsIsFlushedAtCompletion() {
        // A flow that ends mid-run — the collecting coroutine cancelled, or a
        // provider that flushed without a terminal event — still owes its
        // consumer the latest snapshot. Dropping it leaves the live transcript
        // one word short of what the engine actually heard, which is exactly the
        // sort of small wrongness this operator exists to avoid.
        val result = conflate(
            SpeechEvent.Listening,
            SpeechEvent.Partial("open fi"),
            SpeechEvent.Partial("open file"),
        )

        assertEquals(
            listOf(
                SpeechEvent.Listening,
                SpeechEvent.Partial("open file"),
            ),
            result,
        )
    }

    @Test
    fun aFlowWithNothingToConflateIsUnchanged() {
        val input = listOf(
            SpeechEvent.Preparing,
            SpeechEvent.Listening,
            SpeechEvent.Final("done", 10L, 20L),
        )

        assertEquals(input, conflate(*input.toTypedArray()))
    }

    @Test
    fun aSingleSnapshotRunIsNotDelayed() {
        val result = conflate(
            SpeechEvent.Partial("only"),
            SpeechEvent.Final("only", 0L, 50L),
        )

        assertEquals(2, result.size)
        assertEquals(SpeechEvent.Partial("only"), result[0])
    }

    @Test
    fun conflationNeverInventsOrDuplicatesAnEvent() {
        val partials = (1..50).map { SpeechEvent.Partial("w$it") }
        val result = conflate(*partials.toTypedArray())

        assertEquals("exactly one snapshot per run, always the last", 1, result.size)
        assertEquals(
            "and it is the latest one, not merely the first",
            SpeechEvent.Partial("w50"),
            result.single(),
        )
    }
}