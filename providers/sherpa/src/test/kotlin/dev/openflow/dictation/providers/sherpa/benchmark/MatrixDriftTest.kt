package dev.openflow.dictation.providers.sherpa.benchmark

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The in-code benchmark matrix, against the one `docs/providers/model-selection.md` states.
 *
 * `providers/sherpa/build.gradle.kts` claims this test exists and reads that document, so it
 * does — and it is a real check rather than a formality, which is asserted from both
 * directions below. The failure it exists to prevent is quiet and total: a ninth candidate
 * added to the plan's matrix line and not to [ModelMatrix] would be absent from every
 * benchmark run this project ever does, and nothing anywhere would report the gap.
 *
 * Four things are ratcheted, and each is a way the two could otherwise disagree:
 *
 * 1. **The membership and the order** of the plan's matrix line. Compared element by element,
 *    not as sets, because the plan's order is its order and a reader compares this list to the
 *    document by eye.
 * 2. **The mode** each candidate is measured in — online streaming or offline
 *    VAD-segmented. Read from the plan's candidate table. This is the field that decides *which
 *    measurement code runs*, and it is the one whose drift would produce a confident number for
 *    a system nobody ships.
 * 3. **The threshold transcription.** `Thresholds` copies the plan's numbers into code, and a
 *    copy is exactly the kind of thing that ages into a second, softer standard.
 * 4. **The protocol constants.** The plan fixes 5 warmup decodes and a median of ≥20
 *    iterations; the manifest validates against those numbers, so both must match the document.
 */
class MatrixDriftTest {

    private val plan: String = File("docs/providers/model-selection.md").readText()

    // ---- 1. The matrix line -------------------------------------------------------

    /**
     * The candidates named in the plan's `**Matrix:**` line, in order, with the trailing
     * qualifier on `parakeet-tdt-v2 (en only)` stripped.
     *
     * Parsed from the `{ … }` list rather than from the whole line so a later sentence
     * mentioning a model name cannot quietly join the matrix.
     */
    private fun plannedEntries(): List<String> {
        val line = plan.lineSequence().firstOrNull { it.trimStart().startsWith("**Matrix:**") }
            ?: throw AssertionError("docs/providers/model-selection.md has no '**Matrix:**' line. The harness reads the candidate list from it; if the line was renamed, this test and BenchmarkManifestReader.template() need updating together.")
        // From the first `{` to the **first** `}` after it, not the last: the same line ends with
        // a device list in its own braces, and taking the last one folded three device classes
        // into the candidate list — which is how a test meant to prove the matrix has drifted can
        // itself read a matrix nobody wrote.
        val braced = line.substringAfter('{', "").substringBefore('}', "")
        if (braced.isBlank()) {
            throw AssertionError("The '**Matrix:**' line has no { … } candidate list: $line")
        }
        return braced.split(',')
            .map { it.trim().substringBefore('(').trim() }
            .filter { it.isNotEmpty() }
    }

    @Test
    fun thePlanNamesEightCandidatesAndSoDoesTheCode() {
        // The plan writes "{ … eight names … } × {devices} × num_threads ∈ {2, 4}". A count
        // assertion on both sides catches a matrix that grows on either side alone.
        assertEquals(
            "docs/providers/model-selection.md's matrix line does not name eight candidates",
            8,
            plannedEntries().size
        )
        assertEquals(
            "ModelMatrix.PLANNED must be exactly what the plan's matrix line names",
            plannedEntries(),
            ModelMatrix.PLANNED.map { it.planToken }
        )
    }

    @Test
    fun everyPlannedCandidateNamesItsOwnPlanToken() {
        // The membership check above already compares the token lists; this one fails with the
        // specific candidate attached, because "the lists differ" is not an actionable message
        // when a run takes an afternoon.
        val entries = plannedEntries()
        for (candidate in ModelMatrix.PLANNED) {
            assertTrue(
                "ModelMatrix has '${candidate.displayName}' with planToken='${candidate.planToken}', which the " +
                    "plan's matrix line does not name. Either the plan's line or ModelMatrix has drifted; they " +
                    "are the same list and this test exists so they cannot stop being one.",
                entries.contains(candidate.planToken)
            )
        }
    }

    @Test
    fun aQualityReferenceIsACandidateAndIsNotItselfAMatrixRow() {
        // `+2 absolute versus the larger model in family` needs the larger model to be
        // runnable, which is why the bilingual 2023-02-20 zipformer is in CANDIDATES with an
        // empty planToken. It must not be a matrix row of its own — the plan names eight.
        val unplanned = ModelMatrix.CANDIDATES.filter { it.planToken.isEmpty() }
        assertEquals(
            "Exactly one candidate is a quality reference rather than a matrix row",
            1,
            unplanned.size
        )
        assertEquals(
            "ModelMatrix.CANDIDATES is the plan's rows plus its quality references",
            ModelMatrix.PLANNED.size + unplanned.size,
            ModelMatrix.CANDIDATES.size
        )
        for (candidate in ModelMatrix.CANDIDATES) {
            val referenceId = candidate.qualityReferenceId ?: continue
            val reference = ModelMatrix.find(referenceId)
                ?: throw AssertionError("${candidate.displayName} names quality reference '$referenceId', which is not a candidate. The +2-absolute rule would compare it to nothing.")
            assertTrue(
                "${candidate.displayName} is ${candidate.family} but its quality reference ${reference.displayName} " +
                    "is ${reference.family}. The rule is 'the larger model in family'; across families it has no meaning.",
                candidate.family == reference.family
            )
            assertTrue(
                "The matrix lists ${candidate.displayName} before its own quality reference ${reference.displayName}; " +
                    "the reference is the larger model, so it must come later for a reader comparing the two.",
                ModelMatrix.CANDIDATES.indexOf(candidate) < ModelMatrix.CANDIDATES.indexOf(reference)
            )
        }
    }

    // ---- 2. The mode each candidate is measured in ----------------------------------

    @Test
    fun everyCandidatesModeInThePlanMatchesTheFamilyTheHarnessDrives() {
        val rows = candidateTable()
        for (candidate in ModelMatrix.CANDIDATES) {
            val row = rows.filter { it.modelNormalized.contains(normalize(candidate.id)) }
            assertEquals(
                "docs/providers/model-selection.md's candidate table has ${row.size} rows matching " +
                    "'${candidate.id}'. Exactly one is required; two means the match is ambiguous and the " +
                    "mode check below is checking nothing.",
                1,
                row.size
            )
            val mode = row.single().mode
            val online = mode.startsWith("Online")
            assertEquals(
                "The plan calls ${candidate.displayName} '$mode' but ModelMatrix drives it as " +
                    "${candidate.family} (${if (candidate.family.isStreaming) "streaming" else "offline"}). This is " +
                    "the field that decides which measurement code runs: a streaming model scored on a " +
                    "whole-file decode produces a confident number for a system nobody ships.",
                online,
                candidate.family.isStreaming
            )
            assertEquals(
                "The plan calls ${candidate.displayName} '$mode', so its scoring strategy should be " +
                    "${if (online) ScoringStrategy.CHUNKED_STREAMING else ScoringStrategy.VAD_SEGMENTED}",
                if (online) ScoringStrategy.CHUNKED_STREAMING else ScoringStrategy.VAD_SEGMENTED,
                candidate.family.scoringStrategy
            )
        }
    }

    // ---- 3. The thresholds --------------------------------------------------------

    @Test
    fun theThresholdConstantsAreThePlansOwn() {
        // `model-selection.md` writes: "RTF ≤ 0.3 at num_threads=4 on the weakest tier". Thresholds
        // copies that into code. The copy is a ratchet, and it is the one that must not drift:
        // the map settled that the bar stays exactly as written and stays unmeasured rather than
        // being redefined to fit whatever phone is to hand, so a threshold that quietly became
        // 0.4 in code would be a second, softer standard nobody agreed to.
        assertTrue(
            "The plan no longer states 'RTF ≤ 0.3 at num_threads=4'; Thresholds was copied from it and the " +
                "copy needs re-reading against the document, not adjusting to fit.",
            plan.contains("RTF ≤ 0.3 at num_threads=4 on the weakest tier")
        )
        assertEquals(0.3, Thresholds.RTF_MAX_AT_FOUR_THREADS, 0.0)
        assertEquals(4, Thresholds.RTF_THREADS)
        assertEquals(DeviceTier.WEAKEST, Thresholds.REQUIRED_TIER)

        assertTrue(
            "The plan no longer states 'first partial ≤ 500 ms'.",
            plan.contains("first partial ≤ 500 ms")
        )
        assertEquals(500.0, Thresholds.FIRST_PARTIAL_MAX_MS, 0.0)

        assertTrue(
            "The plan no longer states 'Final within ~1 s of endpoint/VAD-segment close'.",
            plan.contains("Final within ~1 s of endpoint/VAD-segment close")
        )
        assertEquals(1000.0, Thresholds.FINAL_AFTER_ENDPOINT_MAX_MS, 0.0)

        assertTrue(
            "The plan no longer states 'WER regression vs the larger model in family ≤ +2 absolute'.",
            plan.contains("WER regression vs the larger model in family ≤ +2 absolute")
        )
        assertEquals(2.0, Thresholds.WER_REGRESSION_MAX_ABSOLUTE_POINTS, 0.0)

        assertTrue(
            "The plan no longer states 'peak RSS ≤ 1 GB on 4 GB devices'.",
            plan.contains("peak RSS ≤ 1 GB on 4 GB devices")
        )
        assertEquals(1_073_741_824L, Thresholds.PEAK_RSS_MAX_BYTES)
    }

    @Test
    fun thePlanStillRecordsThatTheThresholdIsUnmeasurable() {
        // The bar is unmeasurable because there is no weakest tier, and the plan says so in a
        // blockquote. If that paragraph is ever deleted, the harness would go on reporting
        // INDICATIVE_ONLY against a bar the document no longer admits is open — and the next
        // person to read it would take the bar at face value.
        assertTrue(
            "docs/providers/model-selection.md no longer records that the thresholds are unmeasured and one " +
                "unmeasurable, and that the bar stands exactly as written rather than being moved to fit the " +
                "hardware. That paragraph is the harness's licence to report a directional number without " +
                "calling it a gate result.",
            plan.contains("These thresholds are unmeasured, and one of them is currently unmeasurable") &&
                plan.contains("not** redefined to fit whatever phone is to hand") &&
                plan.contains("aspirational until hardware exists")
        )
    }

    // ---- 4. The protocol constants ------------------------------------------------

    @Test
    fun theProtocolConstantsAreThePlansOwn() {
        // BenchmarkManifestReader validates a manifest against these, so a mismatch would let a
        // run print protocol numbers the plan did not specify — a median over 8 iterations
        // reported under a sentence promising ≥20.
        val method = plan.lineSequence().firstOrNull { it.contains("**Method:**") }
            ?: throw AssertionError("docs/providers/model-selection.md has no '**Method:**' line; the harness's protocol constants were copied from it.")
        assertTrue(
            "The plan's Method no longer states 5 discarded warmup decodes: $method",
            method.contains("5 warmup decodes discarded")
        )
        assertTrue(
            "The plan's Method no longer states a median of at least 20 iterations: $method",
            method.contains("median of ≥20 iterations")
        )
        assertTrue(
            "The plan's Method no longer fixes provider=\"cpu\": $method",
            method.contains("provider=\"cpu\"")
        )
        assertTrue(
            "The plan's Method no longer fixes a 16 kHz mono eval set: $method",
            method.contains("fixed 16 kHz mono eval set")
        )
        assertTrue(
            "The plan's Method no longer says int8 variants only: $method",
            method.contains("int8 variants only")
        )
        assertEquals(5, BenchmarkManifestReader.MIN_WARMUP_ITERATIONS)
        assertEquals(20, BenchmarkManifestReader.MIN_MEASURED_ITERATIONS)
    }

    @Test
    fun theThreadSweepIsTheOneThePlanNames() {
        assertTrue(
            "The plan's matrix line no longer says 'num_threads ∈ {2, 4}'.",
            plan.lineSequence().any { it.contains("num_threads ∈ {2, 4}") }
        )
        // The manifest's template is the value a run starts from.
        assertTrue(
            "BenchmarkManifestReader.template() must write the plan's thread sweep",
            BenchmarkManifestReader.template().contains("threads = 2, 4")
        )
    }

    // ---- parsing the candidate table -----------------------------------------------

    private data class CandidateRow(val modelNormalized: String, val mode: String)

    /**
     * The plan's *Candidate model families* table, as (normalized model cell, mode cell) pairs.
     *
     * The first column is normalized by stripping everything that is not a letter or a digit,
     * because the plan's model names carry the `sherpa-onnx-` prefix, an `-int8` suffix and
     * sometimes a variant in the same cell (`sherpa-onnx-whisper-tiny.en` / `tiny`) while
     * `ModelCandidate.id` carries the bare release name. Matching on a normalized containment
     * rather than on exact equality is what lets the check compare identity without the two
     * documents having to agree on a naming convention — and it is unambiguous, which
     * `everyCandidatesModeInThePlanMatchesTheFamilyTheHarnessDrives` asserts rather than assumes:
     * `parakeet-tdt-0.6b-v2` and `parakeet-tdt-0.6b-v3` differ in exactly the characters that
     * survive normalization.
     */
    private fun candidateTable(): List<CandidateRow> =
        plan.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("|") && it.contains("`sherpa-onnx-") }
            .map { line ->
                val cells = line.split('|').map { it.trim() }.filter { it.isNotEmpty() }
                CandidateRow(normalize(cells[0]), cells[1])
            }
            .toList()

    private fun normalize(text: String): String =
        text.lowercase().filter { it.isLetterOrDigit() }
}
