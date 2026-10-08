package dev.openflow.dictation.providers.sherpa.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The verdict logic, with the failing cases asserted rather than assumed.
 *
 * **Every assertion below that ends in `NOT_MET` is a proof that this checker can fail.** A
 * checker nobody has seen reject anything is not a checker: the project has already shipped
 * gates that passed because nothing was reading them. So this file deliberately constructs runs
 * that violate each bound and requires the verdict to say so.
 *
 * The case the map cares most about is the RTF bar, and the assertion that matters is
 * [aRunOnAMidTierPhoneCannotPassTheBarWrittenAgainstTheWeakestTier]: the number may be good,
 * the bound may be satisfied numerically, and the verdict must still be *not a gate result*.
 */
class ThresholdVerdictTest {

    private val zipformer20m = ModelMatrix.require("streaming-zipformer-en-20M-2023-02-17")
    private val zipformerBig = ModelMatrix.require("streaming-zipformer-en-2023-06-26")
    private val whisper = ModelMatrix.require("whisper-tiny.en")

    private fun device(tier: DeviceTier, name: String = "test-phone") =
        DeviceRecord(name, tier, "arm64-v8a", "test-skin", 14, "")

    private fun cell(
        candidate: ModelCandidate = zipformer20m,
        numThreads: Int = 4,
        tier: DeviceTier = DeviceTier.WEAKEST,
        rtfMedian: Double? = 0.20,
        firstPartialMs: Double? = 300.0,
        endpointToFinalMs: Double? = null,
        peakRssBytes: Long? = 400L * 1_048_576,
        errorPercent: Double? = 8.0,
        scored: Int = 20,
        total: Int = 20,
    ) = CellResult(
        candidate = candidate,
        numThreads = numThreads,
        deviceTier = tier,
        scoringStrategy = candidate.family.scoringStrategy,
        rtf = rtfMedian?.let { Summary.of(List(20) { it.toDouble() }) },
        firstPartialMs = firstPartialMs,
        partialIntervalMs = firstPartialMs?.let { 200.0 },
        endpointToFinalMs = endpointToFinalMs,
        speechEndToFinalMs = 420.0,
        errorRate = errorPercent?.let {
            ErrorRate(ScoreUnit.WORD, 2000, (it * 20).toInt(), 0, 0, it)
        },
        peakRssBytes = peakRssBytes,
        peakPssKb = peakRssBytes?.div(1024),
        modelLoadMs = 900.0,
        batteryPercentPerMinute = null,
        utterancesScored = scored,
        utterancesTotal = total,
        thermalBefore = "none(0)",
        thermalAfter = "none(0)",
    )

    private fun evaluate(vararg cells: CellResult, devices: List<String> = listOf("test-phone")) =
        ThresholdVerdictEvaluator.evaluate(cells.toList(), devices)

    private fun ThresholdVerdict.check(name: String): ThresholdCheck =
        checks.firstOrNull { it.name == name }
            ?: throw AssertionError("no check named '$name'. Checks: ${checks.map { it.name }}")

    private fun ThresholdCheck.assertOutcome(expected: ThresholdOutcome) =
        assertEquals("$name: $measured against $threshold came out $outcome, not $expected", expected, outcome)

    // ---- the RTF bar ---------------------------------------------------------------

    @Test
    fun aRunOnTheWeakestTierInsideTheBoundIsAGateResult() {
        val verdict = evaluate(cell(rtfMedian = 0.29))
        assertEquals(GateStatus.GATE_RESULT, verdict.status)
        verdict.check("RTF").assertOutcome(ThresholdOutcome.MET)
        assertTrue(verdict.statement.startsWith("Gate result."))
    }

    @Test
    fun rtfAboveTheBoundOnTheWeakestTierFails() {
        // 0.61 against a 0.3 bar. This is the check proving the checker can fail.
        val verdict = evaluate(cell(rtfMedian = 0.61))
        assertEquals(GateStatus.GATE_RESULT, verdict.status)
        verdict.check("RTF").assertOutcome(ThresholdOutcome.NOT_MET)
    }

    @Test
    fun theWorstAnchoredCellDecidesTheRtfBar() {
        // The bar is a property of the release, not of a lucky run. One candidate at 0.29 and
        // another at 0.61 is a failed matrix; taking the minimum would report it as passing.
        val verdict = evaluate(
            cell(candidate = zipformer20m, rtfMedian = 0.29),
            cell(candidate = zipformerBig, rtfMedian = 0.61),
        )
        verdict.check("RTF").assertOutcome(ThresholdOutcome.NOT_MET)
        assertTrue(verdict.check("RTF").note.contains("worst"))
    }

    @Test
    fun rtfAtTheWrongThreadCountIsNotTheBarThePlanStates() {
        // The bar names `num_threads=4`. A beautiful 0.11 at 2 threads is a different
        // measurement, so the run that only has it has not evaluated the bar.
        val verdict = evaluate(cell(numThreads = 2, rtfMedian = 0.11))
        verdict.check("RTF").assertOutcome(ThresholdOutcome.UNVERIFIABLE)
    }

    @Test
    fun aRunOnAMidTierPhoneCannotPassTheBarWrittenAgainstTheWeakestTier() {
        // The case the map settled. The project has one or two phones, so most runs are on a
        // device that is not the weakest tier. The figures are real; the verdict must still say
        // INDICATIVE ONLY, and must name the device, because `model-selection.md` says a
        // single-device figure is not a gate result and must not be labelled as one.
        val verdict = evaluate(cell(tier = DeviceTier.MID, rtfMedian = 0.19), devices = listOf("Pixel 6a"))
        assertEquals(GateStatus.INDICATIVE_ONLY, verdict.status)
        verdict.check("RTF").assertOutcome(ThresholdOutcome.UNVERIFIABLE)
        assertTrue("the statement must name the device", verdict.statement.contains("Pixel 6a"))
        assertTrue("the statement must name the tier it is standing in for", verdict.statement.contains("mid"))
        assertTrue(
            "the statement must say the bar was not moved to fit the hardware",
            verdict.statement.contains("exactly as written and unmeasured"),
        )
    }

    @Test
    fun aHeadroomPhoneIsAlsoNotTheWeakestTier() {
        val verdict = evaluate(cell(tier = DeviceTier.HEADROOM, rtfMedian = 0.05))
        assertEquals(GateStatus.INDICATIVE_ONLY, verdict.status)
        verdict.check("RTF").assertOutcome(ThresholdOutcome.UNVERIFIABLE)
    }

    @Test
    fun aRunThatCoversTheWeakestTierAmongOthersIsAGateResult() {
        // Adding a second, faster phone to the run must not demote the cell that was on the
        // weakest tier. Coverage is what decides the status, not the number of devices.
        val verdict = evaluate(
            cell(tier = DeviceTier.WEAKEST, rtfMedian = 0.28),
            cell(tier = DeviceTier.HEADROOM, rtfMedian = 0.04),
            devices = listOf("weak-phone", "fast-phone"),
        )
        assertEquals(GateStatus.GATE_RESULT, verdict.status)
        assertEquals(setOf(DeviceTier.WEAKEST, DeviceTier.HEADROOM), verdict.tiersCovered)
    }

    @Test
    fun anUnmeasuredRtfIsUnverifiableRatherThanPassed() {
        val verdict = evaluate(cell(rtfMedian = null))
        verdict.check("RTF").assertOutcome(ThresholdOutcome.UNVERIFIABLE)
        assertEquals("not measured", verdict.check("RTF").measured)
    }

    // ---- first partial --------------------------------------------------------------

    @Test
    fun aFirstPartialOverFiveHundredMillisecondsFails() {
        evaluate(cell(firstPartialMs = 501.0)).check("first partial — ${zipformer20m.displayName} @ 4t")
            .assertOutcome(ThresholdOutcome.NOT_MET)
    }

    @Test
    fun aFirstPartialAtTheBoundIsInsideIt() {
        evaluate(cell(firstPartialMs = 500.0)).check("first partial — ${zipformer20m.displayName} @ 4t")
            .assertOutcome(ThresholdOutcome.MET)
    }

    @Test
    fun anOfflineModelHasNoFirstPartialAndIsNotFailedForIt() {
        // `whisper-tiny.en` emits no partials at all. That is a fact about the family, and
        // recording it as a failure would put a red mark on a model for a metric it has no
        // obligation to satisfy.
        val check = evaluate(cell(candidate = whisper, firstPartialMs = null))
            .check("first partial — ${whisper.displayName} @ 4t")
        check.assertOutcome(ThresholdOutcome.NOT_APPLICABLE)
        assertTrue(check.note.contains("Only online models emit partials"))
    }

    // ---- endpoint → Final -----------------------------------------------------------

    @Test
    fun anOfflineEndpointToFinalOverOneSecondFails() {
        val verdict = evaluate(cell(candidate = whisper, endpointToFinalMs = 1001.0))
        verdict.check("endpoint→Final — ${whisper.displayName} @ 4t").assertOutcome(ThresholdOutcome.NOT_MET)
    }

    @Test
    fun anOfflineEndpointToFinalInsideOneSecondPasses() {
        evaluate(cell(candidate = whisper, endpointToFinalMs = 999.0))
            .check("endpoint→Final — ${whisper.displayName} @ 4t")
            .assertOutcome(ThresholdOutcome.MET)
    }

    @Test
    fun anOnlineModelIsNotPassedAnEndpointToFinalBoundItSatisfiesByConstruction() {
        // `isEndpoint()` true means `getResult()` already holds the finished text, so this
        // window is a fraction of a millisecond for every online model forever. Recording that
        // as MET would put a green tick in the results document for something nobody measured,
        // which is the failure this assertion exists to prevent.
        val check = evaluate(cell(endpointToFinalMs = null))
            .check("endpoint→Final — ${zipformer20m.displayName} @ 4t")
        check.assertOutcome(ThresholdOutcome.NOT_APPLICABLE)
        assertTrue(check.note.contains("endpoint result IS its Final"))
    }

    // ---- peak RSS -------------------------------------------------------------------

    @Test
    fun peakRssOverOneGibibyteFails() {
        evaluate(cell(peakRssBytes = 1_073_741_825L)).check("peak RSS — ${zipformer20m.displayName} @ 4t")
            .assertOutcome(ThresholdOutcome.NOT_MET)
    }

    @Test
    fun peakRssAtExactlyOneGibibyteIsInsideTheBound() {
        evaluate(cell(peakRssBytes = 1_073_741_824L)).check("peak RSS — ${zipformer20m.displayName} @ 4t")
            .assertOutcome(ThresholdOutcome.MET)
    }

    @Test
    fun anUnmeasuredPeakRssIsNotApplicableRatherThanPassing() {
        evaluate(cell(peakRssBytes = null)).check("peak RSS — ${zipformer20m.displayName} @ 4t")
            .assertOutcome(ThresholdOutcome.NOT_APPLICABLE)
    }

    // ---- the WER regression rule ---------------------------------------------------

    @Test
    fun aWerRegressionOverTwoAbsolutePointsFails() {
        // +2 is the bound, +2.5 is the failure. `model-selection.md` writes the rule in
        // *percentage points*, which is why `errorPercent` is a percentage.
        val verdict = evaluate(
            cell(candidate = zipformer20m, errorPercent = 10.5),
            cell(candidate = zipformerBig, errorPercent = 8.0),
        )
        val check = verdict.check("WER regression")
        check.assertOutcome(ThresholdOutcome.NOT_MET)
        assertTrue(check.note.contains("+2.50 pts"))
    }

    @Test
    fun aWerRegressionOfExactlyTwoPointsIsInsideTheRule() {
        evaluate(
            cell(candidate = zipformer20m, errorPercent = 10.0),
            cell(candidate = zipformerBig, errorPercent = 8.0),
        ).check("WER regression").assertOutcome(ThresholdOutcome.MET)
    }

    @Test
    fun beingBetterThanTheLargerModelIsNotAFailure() {
        evaluate(
            cell(candidate = zipformer20m, errorPercent = 7.0),
            cell(candidate = zipformerBig, errorPercent = 8.0),
        ).check("WER regression").assertOutcome(ThresholdOutcome.MET)
    }

    @Test
    fun aWerRuleWithNoRunnableQualityReferenceIsUnverifiableNotPassed() {
        // "+2 absolute versus the larger model in family" has no referent without the larger
        // model. Reporting "no comparison available" as "no regression" is the substitution this
        // project forbids, so it is UNVERIFIABLE.
        val check = evaluate(cell(candidate = zipformer20m, errorPercent = 99.0)).check("WER regression")
        check.assertOutcome(ThresholdOutcome.UNVERIFIABLE)
        assertTrue(check.note.contains("unverifiable"))
        assertTrue(check.note.contains(zipformerBig.id))
    }

    @Test
    fun aWerRuleWithNeitherSideScoredIsUnverifiable() {
        val check = evaluate(cell(errorPercent = null)).check("WER regression")
        check.assertOutcome(ThresholdOutcome.UNVERIFIABLE)
    }

    // ---- the bounds the plan does not set -------------------------------------------

    @Test
    fun thePlanSetsNoBoundForLoadTimeOrBatterySoNoneIsInvented() {
        // The plan lists model load time and battery drain as metrics and sets no number for
        // either. A reader who cannot tell whether a bound was forgotten or deliberately
        // skipped will assume the former, so the run says which it is.
        val verdict = evaluate(cell())
        verdict.check("model load time").assertOutcome(ThresholdOutcome.NOT_APPLICABLE)
        assertEquals("no bound stated", verdict.check("model load time").threshold)
        verdict.check("battery drain").assertOutcome(ThresholdOutcome.NOT_APPLICABLE)
        assertEquals("no bound stated", verdict.check("battery drain").threshold)
    }

    @Test
    fun anUnmeasuredBatteryFigureIsReportedAsNotMeasuredRatherThanZero() {
        assertEquals("not measured", evaluate(cell()).check("battery drain").measured)
    }

    // ---- the statement --------------------------------------------------------------

    @Test
    fun aRunWithNoCellsCannotBeAGateResult() {
        val verdict = ThresholdVerdictEvaluator.evaluate(emptyList(), emptyList())
        assertEquals(GateStatus.INDICATIVE_ONLY, verdict.status)
        verdict.check("RTF").assertOutcome(ThresholdOutcome.UNVERIFIABLE)
        assertTrue(
            "with no devices named the statement must still say it is not a gate result",
            verdict.statement.contains("INDICATIVE ONLY"),
        )
    }

    @Test
    fun theThresholdTextIsThePlansOwnWordingNotARelaxedRestatement() {
        // If this ever reads 0.4 the harness is asserting a standard nobody agreed to, and this
        // assertion is what says so.
        val rtf = evaluate(cell()).check("RTF")
        assertEquals("RTF ≤ 0.3 at num_threads=4 on the weakest tier", rtf.threshold)
        assertNotEquals("RTF ≤ 0.4 at num_threads=4 on the weakest tier", rtf.threshold)
        assertEquals(
            "docs/providers/model-selection.md — Device-benchmarking plan, Pass thresholds",
            Thresholds.SOURCE,
        )
    }
}
