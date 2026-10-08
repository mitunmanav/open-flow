package dev.openflow.dictation.providers.sherpa.benchmark

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The report, as something a machine reads.
 *
 * The JSON is hand-written rather than produced by a library, so the escaping is the thing worth
 * testing: model display names, OEM skins and a person's free-text notes all reach it, and one
 * raw newline inside a JSON string makes the whole document unparseable — losing the entire run
 * to a formatting slip in a note field. `org.json` is available in the Android unit-test
 * environment, so these tests parse the output rather than pattern-matching it.
 *
 * The other thing tested here is that **null means not measured**. A `0` in any of these columns
 * would read as a measurement, and every one of them has a legitimate zero meaning "not
 * applicable to this family" or "nobody ran this".
 */
class RunReportTest {

    private val device = DeviceRecord("Pixel 6a", DeviceTier.MID, "arm64-v8a", "stock", 14, "")

    private val protocol = Protocol(
        threadCounts = listOf(2, 4),
        warmupIterations = 5,
        measuredIterations = 20,
        chunkMillis = 100,
        trailingSilenceMillis = 2400,
        executionProvider = "cpu",
    )

    private val evalSet = EvalSet(
        dir = "/sdcard/openflow-eval",
        pattern = "*.wav",
        unit = ScoreUnit.WORD,
        reference = ReferenceLayout.LIBRISPEECH_TRANSCRIPT,
        transcriptPath = "/sdcard/openflow-eval/test-clean.trans.txt",
    )

    private fun cell(
        candidate: ModelCandidate,
        rtf: Double?,
        endpointToFinalMs: Double? = null,
        scored: Int = 20,
        total: Int = 20,
        thermal: String = "none(0)",
        notes: List<String> = emptyList(),
    ) = CellResult(
        candidate = candidate,
        numThreads = 4,
        deviceTier = DeviceTier.MID,
        scoringStrategy = candidate.family.scoringStrategy,
        rtf = rtf?.let { Summary.of(List(20) { it.toDouble() }) },
        firstPartialMs = null,
        partialIntervalMs = null,
        endpointToFinalMs = endpointToFinalMs,
        speechEndToFinalMs = null,
        errorRate = null,
        peakRssBytes = null,
        peakPssKb = null,
        modelLoadMs = null,
        batteryPercentPerMinute = null,
        utterancesScored = scored,
        utterancesTotal = total,
        thermalBefore = thermal,
        thermalAfter = thermal,
        notes = notes,
    )

    private fun report(vararg cells: CellResult): RunReport {
        val list = cells.toList()
        return RunReport.of(
            device = device,
            cpuCount = 8,
            requestedThreads = listOf(2, 4),
            protocol = protocol,
            evalSet = evalSet,
            cells = list,
            verdict = ThresholdVerdictEvaluator.evaluate(list, listOf("Pixel 6a")),
            sherpaVersion = "v1.13.8",
            generatedAtEpochMs = 0,
            candidatesNotMeasured = listOf("moonshine-tiny-en-int8"),
            cellsSkipped = listOf("parakeet-tdt-0.6b-v2 @ 4t: encoder.int8.onnx not found"),
        )
    }

    private fun online() = ModelMatrix.require("streaming-zipformer-en-20M-2023-02-17")
    private fun offline() = ModelMatrix.require("whisper-tiny.en")

    // ---- the JSON -------------------------------------------------------------------

    @Test
    fun theReportParses() {
        val json = JSONObject(report(cell(online(), 0.22), cell(offline(), 0.31, endpointToFinalMs = 420.0)).toJson())
        assertEquals(2, json.getJSONArray("cells").length())
        assertEquals("v1.13.8", json.getString("sherpaVersion"))
        assertEquals(8, json.getJSONObject("device").getInt("cpuCount"))
        assertEquals(2, json.getJSONArray("device").getJSONArray(0).length())
        assertEquals("WORD", json.getJSONObject("evalSet").getString("unit"))
        assertEquals("librispeech", json.getJSONObject("evalSet").getString("referenceLayout"))
    }

    @Test
    fun theInstrumentLabelIsFixedAndNotPassedIn() {
        // `model-selection.md` settles that three instruments report into the results document
        // as separate sections, and that the gap between the second and third is the cost of the
        // integration. A number without its instrument is a rumour, so the label cannot be
        // chosen by a caller that might relabel this run as a provider one.
        val json = JSONObject(report(cell(online(), 0.22)).toJson())
        assertEquals(RunReport.INSTRUMENT, json.getString("instrument"))
        assertTrue(json.getString("instrument").contains("instrument 2 of 3"))
        assertTrue(json.getString("instrument").contains("Does NOT measure OpenFlow"))
    }

    @Test
    fun anUnmeasuredMetricIsNullAndNeverZero() {
        val json = JSONObject(report(cell(online(), 0.22)).toJson())
        val onlineCell = json.getJSONArray("cells").getJSONObject(0)
        assertTrue(onlineCell.isNull("firstPartialMs"))
        assertTrue(onlineCell.isNull("errorRate"))
        assertTrue(onlineCell.isNull("peakRssBytes"))
        assertTrue(onlineCell.isNull("modelLoadMs"))
        assertTrue(onlineCell.isNull("batteryPercentPerMinute"))
        assertTrue(onlineCell.isNull("rtf") == false)
    }

    @Test
    fun anOnlineCellCarriesNoEndpointToFinalAndTheOfflineOneDoes() {
        val json = JSONObject(report(cell(online(), 0.22), cell(offline(), 0.31, endpointToFinalMs = 420.0)).toJson())
        val cells = json.getJSONArray("cells")
        assertTrue(cells.getJSONObject(0).isNull("endpointToFinalMs"))
        assertEquals(420.0, cells.getJSONObject(1).getDouble("endpointToFinalMs"), 1e-9)
    }

    @Test
    fun freeTextWithNewlinesQuotesAndStillParses() {
        // A person's `notes` field is the most likely place for a newline, and one raw newline
        // inside a JSON string loses the whole run.
        val json = JSONObject(
            report(
                cell(
                    offline(), 0.31,
                    notes = listOf("battery: start=87% end=64%\nplugged=none", "quote \" and backslash \\"),
                )
            ).toJson()
        )
        val notes: JSONArray = json.getJSONArray("cells").getJSONObject(0).getJSONArray("notes")
        assertEquals("battery: start=87% end=64%\nplugged=none", notes.getString(0))
        assertEquals("quote \" and backslash \\", notes.getString(1))
    }

    @Test
    fun everyControlCharacterIsEscaped() {
        assertEquals("\"a\\u0001b\"", RunReport.quote("a\u0001b"))
        assertEquals("\"a\\u001fb\"", RunReport.quote("a\u001fb"))
        assertEquals("\"tab\\there\"", RunReport.quote("tab\there"))
        assertEquals("\"nl\\nhere\"", RunReport.quote("nl\nhere"))
        assertEquals("\"cr\\rhere\"", RunReport.quote("cr\rhere"))
        assertEquals("\"bs\\bhere\"", RunReport.quote("bs\bhere"))
        assertEquals("\"ff\\fhere\"", RunReport.quote("f\u000chere"))
    }

    @Test
    fun decimalFormattingDoesNotFollowTheDeviceLocale() {
        // A phone set to a locale whose decimal separator is a comma would emit `0,250` into
        // JSON and every number in the report would stop being a number. The report says it
        // formats independently of the platform locale, so this asserts it rather than trusting
        // the sentence.
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("0.250", RunReport.round(0.25, 3))
            assertEquals("1234.5", RunReport.round(1234.5, 1))
            val json = JSONObject(report(cell(online(), 0.25)).toJson())
            assertEquals(
                0.25,
                json.getJSONArray("cells").getJSONObject(0).getJSONObject("rtf").getDouble("median"),
                1e-9,
            )
        } finally {
            java.util.Locale.setDefault(previous)
        }
    }

    @Test
    fun whatTheRunDidNotDoIsInTheMachineReadableHalfToo() {
        // A report whose omissions live only in a paragraph reads as complete when parsed.
        val json = JSONObject(report(cell(online(), 0.22)).toJson())
        assertEquals("moonshine-tiny-en-int8", json.getJSONArray("candidatesNotMeasured").getString(0))
        assertTrue(json.getJSONArray("cellsSkipped").getString(0).contains("not found"))
    }

    @Test
    fun theVerdictTravelsWithTheNumbers() {
        val json = JSONObject(report(cell(online(), 0.22)).toJson())
        val verdict = json.getJSONObject("verdict")
        assertEquals("INDICATIVE_ONLY", verdict.getString("status"))
        assertTrue(verdict.getString("statement").contains("not a gate result"))
        assertEquals(
            "docs/providers/model-selection.md — Device-benchmarking plan, Pass thresholds",
            verdict.getString("thresholdsSource"),
        )
        assertTrue(verdict.getJSONArray("checks").length() > 0)
    }

    @Test
    fun theThreadSweepTravelsWithTheRun() {
        // `threads=2, 4` requested against a 2-core device reads as what happened, not as what
        // was asked for, because both numbers are in the report.
        val json = JSONObject(report(cell(online(), 0.22)).toJson())
        assertEquals(2, json.getJSONArray("device").getJSONArray(0).length())
        assertEquals(8, json.getJSONObject("device").getInt("cpuCount"))
    }

    // ---- the markdown ---------------------------------------------------------------

    @Test
    fun theMarkdownCarriesTheInstrumentAndTheVerdictAboveTheTable() {
        // A table lifted out of this report without its two header lines is a set of numbers with
        // no producer and no standing, which is exactly what the results document exists to
        // prevent.
        val markdown = report(cell(online(), 0.22), cell(offline(), 0.31, endpointToFinalMs = 420.0)).toMarkdown()
        val instrumentAt = markdown.indexOf("API-level harness")
        val verdictAt = markdown.indexOf("INDICATIVE ONLY")
        val tableAt = markdown.indexOf("| model |")
        assertTrue(instrumentAt in 0 until verdictAt)
        assertTrue(verdictAt < tableAt)
    }

    @Test
    fun theMarkdownStatesTheProtocolItRan() {
        val markdown = report(cell(online(), 0.22)).toMarkdown()
        assertTrue(markdown.contains("5 warmup decodes discarded"))
        assertTrue(markdown.contains("median of 20 iterations"))
        assertTrue(markdown.contains("100 ms chunks"))
        assertTrue(markdown.contains("2400 ms trailing silence"))
        assertTrue(markdown.contains("provider=\"cpu\""))
        assertTrue(markdown.contains("RTF is a benchmark-only quantity"))
    }

    @Test
    fun incompleteWerCoverageIsCalledOutAboveTheTable() {
        // WER over a subset is arithmetically valid and completely misleading, so the shortfall
        // is stated rather than left for a reader to infer from a denominator.
        val markdown = report(cell(online(), 0.22, scored = 7, total = 20)).toMarkdown()
        assertTrue(markdown.contains("Incomplete WER coverage"))
    }

    @Test
    fun anUnmeasuredMetricRendersAsADashNotAZero() {
        val markdown = report(cell(online(), 0.22)).toMarkdown()
        val row = markdown.lineSequence().first { it.startsWith("| streaming zipformer en 20M |") }
        assertTrue(row.contains("| — "))
        assertFalse("an offline model has no first partial, which is a fact and not a zero", row.contains("| 0 ms |"))
        assertTrue("and it says so", row.contains("n/a"))
    }

    @Test
    fun whatWasNotRunIsListedInTheMarkdownToo() {
        val markdown = report(cell(online(), 0.22)).toMarkdown()
        assertTrue(markdown.contains("Not measured in this run"))
        assertTrue(markdown.contains("moonshine-tiny-en-int8"))
        assertTrue(markdown.contains("Skipped:"))
    }

    // ---- formatting -----------------------------------------------------------------

    @Test
    fun fixedDecimalFormattingDoesNotVaryWithThePlatformLocale() {
        // A comma decimal separator would make every number in the JSON unparseable.
        assertEquals("0.250", RunReport.round(0.25, 3))
        assertEquals("1234.5", RunReport.round(1234.5, 1))
        assertEquals("0", RunReport.round(0.0, 0))
    }

    @Test
    fun aCellIsNamedTheSameWayEveryTime() {
        val cell = cell(online(), 0.22)
        assertEquals("streaming zipformer en 20M @ 4t", cell.label)
        assertTrue(cell.scoredWholeEvalSet)
        assertFalse(cell(online(), 0.22, scored = 1, total = 2).scoredWholeEvalSet)
    }
}
