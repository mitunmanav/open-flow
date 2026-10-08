package dev.openflow.dictation.providers.sherpa.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The summary every cell's headline number comes out of.
 *
 * The protocol is the plan's: 5 warmup decodes discarded, then the **median** of at least 20
 * iterations. The median rather than the mean because a phone thermally throttles partway
 * through a 20-iteration sweep and RTF climbs as it does, and a mean would report that climb
 * as if it were the model's cost.
 */
class StatisticsTest {

    @Test
    fun anEmptySampleIsAbsentRatherThanZero() {
        // Empty is the absence of evidence. A run that measured nothing has not measured zero,
        // and zero RTF would read as impossibly fast rather than as nothing.
        assertNull(Summary.of(emptyList()))
    }

    @Test
    fun theMedianOfAnOddSampleIsTheMiddleOne() {
        val summary = Summary.of(listOf(5.0, 1.0, 3.0))!!
        assertEquals(3, summary.n)
        assertEquals(3.0, summary.median, 0.0)
        assertEquals(1.0, summary.min, 0.0)
        assertEquals(5.0, summary.max, 0.0)
        assertEquals(3.0, summary.mean, 1e-9)
    }

    @Test
    fun theMedianOfAnEvenSampleAveragesTheTwoMiddleOnes() {
        // The one place a reported value is not an observation, and the convention every reader
        // of a median expects. `Summary.percentile` below is the opposite rule on purpose.
        assertEquals(2.5, Summary.of(listOf(1.0, 2.0, 3.0, 4.0))!!.median, 0.0)
    }

    @Test
    fun aPercentileIsNearestRankSoItIsAlwaysASampleThatHappened() {
        // With 20 samples, an interpolated p90 is a value no iteration produced, and the whole
        // point of this harness is to report what was measured.
        val samples = (1..20).map { it.toDouble() }
        val sorted = samples.sorted()
        assertEquals(18.0, Summary.percentile(sorted, 0.90), 0.0)
        assertEquals(1.0, Summary.percentile(sorted, 0.0), 0.0)
        assertEquals(20.0, Summary.percentile(sorted, 1.0), 0.0)
        // Nearest-rank means the returned value is one of the observations, for every width the
        // protocol allows — not a value interpolated between two of them.
        for (n in 1..40) {
            val values = (1..n).map { it.toDouble() }
            for (p in listOf(0.5, 0.75, 0.9, 0.95, 1.0)) {
                assertTrue(
                    "percentile($p) of $n samples returned a value that is not one of them",
                    Summary.percentile(values.sorted(), p) in values,
                )
            }
        }
    }

    @Test
    fun thePlanTwentyIterationFloorHasEnoughSamplesToMakeTheMedianAndP90Meaningful() {
        val twenty = (1..20).map { it.toDouble() }
        val summary = Summary.of(twenty)!!
        assertEquals(20, summary.n)
        // 1..20: the median is the mean of 10 and 11; p90 is the 18th sample.
        assertEquals(10.5, summary.median, 0.0)
        assertEquals(18.0, summary.p90, 0.0)
    }

    @Test
    fun theSpreadBesideTheMedianIsKeptBecauseItChangesWhatTheMedianMeans() {
        // A cell whose p90 is three times its median is a different measurement from one where
        // they agree, and a single number cannot say which.
        val steady = Summary.of(List(20) { 0.20 })!!
        val throttling = Summary.of((1..20).map { 0.20 + it * 0.05 })!!
        assertEquals(steady.median, steady.p90, 1e-9)
        assertTrue(throttling.p90 > throttling.median * 2)
    }

    @Test
    fun anEmptySampleHasNoMedianOrPercentile() {
        for (block in listOf<() -> Unit>(
            { Summary.median(emptyList()) },
            { Summary.percentile(emptyList(), 0.5) },
        )) {
            try {
                block()
                throw AssertionError("expected an IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.contains("empty"))
            }
        }
    }

    @Test
    fun aPercentileOutsideZeroToOneIsRefused() {
        try {
            Summary.percentile(listOf(1.0), 1.5)
            throw AssertionError("expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("0..1"))
        }
    }
}
