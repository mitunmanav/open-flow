package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * The pass thresholds, transcribed from `model-selection.md`'s *Pass thresholds*.
 *
 * They are written here as code, and the plan's own sentence above them says
 * *"proposed, tuned after first run"* — so this list is a **transcription, not an
 * endorsement**. It is kept literal on purpose. The map settled that these numbers
 * stay exactly as written and stay unmeasured rather than being redefined to fit
 * whatever phone is to hand, so the only legitimate reason for any of them to
 * change is a first run that says the threshold was wrong, recorded as a decision.
 * A harness that quietly relaxed `0.3` to `0.4` because a device missed it would
 * destroy the only thing the number was for.
 */
object Thresholds {

    const val SOURCE = "docs/providers/model-selection.md — Device-benchmarking plan, Pass thresholds"

    /** "RTF ≤ 0.3 at num_threads=4 on the weakest tier". */
    const val RTF_MAX_AT_FOUR_THREADS = 0.3
    const val RTF_THREADS = 4

    /** "first partial ≤ 500 ms". */
    const val FIRST_PARTIAL_MAX_MS = 500.0

    /**
     * "Final within ~1 s of endpoint/VAD-segment close".
     *
     * `~1 s` is transcribed as 1000 ms. It is the one threshold in the list that
     * is approximate in the source, and it is applied as a hard bound anyway —
     * a threshold written with a tilde cannot be a soft one in code, or it would
     * pass everything and discriminate nothing.
     */
    const val FINAL_AFTER_ENDPOINT_MAX_MS = 1000.0

    /** "WER regression vs the larger model in family ≤ +2 absolute on test-clean". */
    const val WER_REGRESSION_MAX_ABSOLUTE_POINTS = 2.0

    /** "peak RSS ≤ 1 GB on 4 GB devices". */
    const val PEAK_RSS_MAX_BYTES = 1_073_741_824L

    /** The tier the RTF bar is written against, and so the one a gate result must cover. */
    val REQUIRED_TIER: DeviceTier = DeviceTier.WEAKEST
}

/** How one threshold came out. */
enum class ThresholdOutcome {
    /** Measured, and inside the bound. */
    MET,

    /** Measured, and outside the bound. */
    NOT_MET,

    /** The bound does not apply to this cell — e.g. first-partial on an offline model. */
    NOT_APPLICABLE,

    /**
     * The bound cannot be evaluated on this run, and the reason is not that the
     * model failed.
     *
     * Distinct from [NOT_MET] because the two call for opposite responses: a
     * failure means change the model, an unverifiable bound means get more
     * hardware. Collapsing them is how an unmeasured threshold quietly becomes a
     * passing one.
     */
    UNVERIFIABLE,
}

/**
 * Whether a run may be reported as a gate result.
 *
 * The distinction the plan insists on: *"a single-device figure is not a gate
 * result and must not be labelled as one."*
 */
enum class GateStatus {
    /** Covers the weakest tier, so it can speak for the destination's promise. */
    GATE_RESULT,

    /** Directional only. Must name the device and the tier it stands in for. */
    INDICATIVE_ONLY,
}

/** One threshold, one cell, one verdict. */
data class ThresholdCheck(
    val name: String,
    /** The bound exactly as the plan states it. */
    val threshold: String,
    /** What was measured, in the same unit. */
    val measured: String,
    val outcome: ThresholdOutcome,
    val note: String,
)

/**
 * The whole run's standing against the plan's thresholds.
 *
 * [statement] is generated, not written, so it cannot describe a run more
 * favourably than [tiersCovered] supports.
 */
data class ThresholdVerdict(
    val status: GateStatus,
    val checks: List<ThresholdCheck>,
    val tiersCovered: Set<DeviceTier>,
    val devices: List<String>,
    /** The prose a reader of `model-benchmark-results.md` must see above the numbers. */
    val statement: String,
)

/**
 * Turns measured cells into a verdict, and refuses to overstate it.
 *
 * The rule that shapes everything here: **the RTF bound is written against the
 * weakest tier, so a run that did not include the weakest tier cannot evaluate
 * it** — not "evaluated it optimistically", not "evaluated it against whatever
 * was to hand". It reports [ThresholdOutcome.UNVERIFIABLE], names the tier that
 * was run instead, and marks the whole run [GateStatus.INDICATIVE_ONLY].
 *
 * That is the same refusal the map applies to the acceptance gate's missing
 * device classes: report the gap, never redefine the promise to close it. A
 * project with one phone can produce excellent directional numbers; what it
 * cannot do is convert them into a verdict about a tier it has never run.
 */
object ThresholdVerdictEvaluator {

    fun evaluate(cells: List<CellResult>, devices: List<String>): ThresholdVerdict {
        val tiersCovered = cells.map { it.deviceTier }.toSet()
        val coversWeakest = Thresholds.REQUIRED_TIER in tiersCovered
        val status = if (coversWeakest) GateStatus.GATE_RESULT else GateStatus.INDICATIVE_ONLY

        return ThresholdVerdict(
            status = status,
            checks = buildList {
                add(rtfCheck(cells))
                addAll(
                    perCell(cells, "first partial", "≤ ${Thresholds.FIRST_PARTIAL_MAX_MS.toInt()} ms", Thresholds.FIRST_PARTIAL_MAX_MS, { it.firstPartialMs }) { "${it.toLong()} ms" }
                )
                addAll(
                    perCell(cells, "endpoint→Final", "≤ ${Thresholds.FINAL_AFTER_ENDPOINT_MAX_MS.toInt()} ms", Thresholds.FINAL_AFTER_ENDPOINT_MAX_MS, { it.endpointToFinalMs }) { "${it.toLong()} ms" }
                )
                addAll(
                    perCell(cells, "peak RSS", "≤ 1 GB", Thresholds.PEAK_RSS_MAX_BYTES.toDouble(), { it.peakRssBytes?.toDouble() }) { "%.0f MB".format(it / 1_048_576.0) }
                )
                add(werRegressionCheck(cells))
                // Two thresholds the plan states that have no column above. Said so
                // rather than omitted: a reader who cannot tell whether a bound was
                // forgotten or deliberately skipped will assume the former.
                add(
                    ThresholdCheck(
                        name = "model load time",
                        threshold = "no bound stated",
                        measured = cells.mapNotNull { it.modelLoadMs }.let { if (it.isEmpty()) "not measured" else "${it.size} cell(s) recorded" },
                        outcome = ThresholdOutcome.NOT_APPLICABLE,
                        note = "The plan lists model load time as a metric but sets no threshold for it, so this " +
                            "harness reports the number and asserts nothing about it.",
                    )
                )
                add(
                    ThresholdCheck(
                        name = "battery drain",
                        threshold = "no bound stated",
                        measured = cells.firstOrNull { it.batteryPercentPerMinute != null }
                            ?.let { "${round2(it.batteryPercentPerMinute!!)} %/min" } ?: "not measured",
                        outcome = ThresholdOutcome.NOT_APPLICABLE,
                        note = "The plan says \"one device, one battery-tier model\" and sets no number. A drain " +
                            "figure from a charging phone is worse than none, so the run records whether it was " +
                            "plugged in and this check reports nothing either way.",
                    )
                )
            },
            tiersCovered = tiersCovered,
            devices = devices,
            statement = statement(status, tiersCovered, devices),
        )
    }

    /**
     * RTF is judged on the **worst** anchored cell, not the best.
     *
     * The bar is a property of the release, not of a lucky run: a matrix where
     * one candidate at 4 threads on the weakest tier comes in at 0.29 and another
     * at 0.61 has failed, and taking the minimum would report the release as
     * passing. Which cells are anchored is stated in the note so a reader can see
     * the comparison being made rather than trusting the verdict alone.
     */
    private fun rtfCheck(cells: List<CellResult>): ThresholdCheck {
        val threshold = "RTF ≤ ${Thresholds.RTF_MAX_AT_FOUR_THREADS} at num_threads=${Thresholds.RTF_THREADS} on the ${Thresholds.REQUIRED_TIER.manifestValue} tier"
        val anchored = cells.filter {
            it.numThreads == Thresholds.RTF_THREADS && it.deviceTier == Thresholds.REQUIRED_TIER
        }
        val medians = anchored.mapNotNull { it.rtf?.median }
        val worst = medians.maxOrNull()
        val tiersRun = cells.map { it.deviceTier.manifestValue }.distinct().sorted().joinToString(", ")

        return when {
            medians.isEmpty() -> ThresholdCheck(
                name = "RTF",
                threshold = threshold,
                measured = "not measured",
                outcome = ThresholdOutcome.UNVERIFIABLE,
                note = "No cell covered num_threads=${Thresholds.RTF_THREADS} on the " +
                    "${Thresholds.REQUIRED_TIER.manifestValue} tier (tiers present: $tiersRun). The bar is written " +
                    "against that tier and has no other referent; measuring it elsewhere does not evaluate it.",
            )

            Thresholds.REQUIRED_TIER !in cells.map { it.deviceTier } -> ThresholdCheck(
                name = "RTF",
                threshold = threshold,
                measured = "median ${round3(worst!!)} (worst of ${medians.size} cell(s), tier(s) $tiersRun)",
                outcome = ThresholdOutcome.UNVERIFIABLE,
                note = "The figures are real and directional. They are not a verdict on the bar, which is written " +
                    "against the ${Thresholds.REQUIRED_TIER.manifestValue} tier this run did not cover.",
            )

            worst!! > Thresholds.RTF_MAX_AT_FOUR_THREADS -> ThresholdCheck(
                name = "RTF",
                threshold = threshold,
                measured = "median ${round3(worst)} (worst of ${medians.size} anchored cell(s))",
                outcome = ThresholdOutcome.NOT_MET,
                note = "Judged on the worst anchored cell, because the bar is a property of the release and not " +
                    "of its luckiest candidate.",
            )

            else -> ThresholdCheck(
                name = "RTF",
                threshold = threshold,
                measured = "median ${round3(worst)} (worst of ${medians.size} anchored cell(s))",
                outcome = ThresholdOutcome.MET,
                note = "Every anchored cell is inside the bound.",
            )
        }
    }

    /**
     * One check per cell, for a metric that has no single number across the run.
     *
     * These are reported per cell rather than aggregated because each is a
     * property of a specific model at a specific thread count; a worst-case rollup
     * would answer a question nobody asked and hide which cell failed.
     */
    private fun perCell(
        cells: List<CellResult>,
        name: String,
        threshold: String,
        bound: Double,
        metric: (CellResult) -> Double?,
        render: (Double) -> String,
    ): List<ThresholdCheck> = cells.map { cell ->
        val value = metric(cell)
        ThresholdCheck(
            name = "$name — ${cell.label}",
            threshold = threshold,
            measured = value?.let(render) ?: "not measured",
            outcome = when {
                value == null -> ThresholdOutcome.NOT_APPLICABLE
                value <= bound -> ThresholdOutcome.MET
                else -> ThresholdOutcome.NOT_MET
            },
            note = if (value == null) notApplicableNote(name, cell) else "",
        )
    }

    private fun notApplicableNote(name: String, cell: CellResult): String = when (name) {
        "first partial" ->
            "Only online models emit partials. ${cell.label} is ${cell.candidate.family}, decoded as " +
                "${cell.candidate.family.scoringStrategy} with one result per segment, so it has no first " +
                "partial to measure. That is a property of the family, not a gap in the run — which is why it " +
                "is recorded as not-applicable instead of leaving the row out."
        else ->
            "${cell.label} produced no value for this metric, so the run says so rather than omitting the row."
    }

    /**
     * "+2 absolute versus the larger model in family", which is a comparison and
     * not a bound, so it is evaluated pairwise against
     * [ModelCandidate.qualityReferenceId] at the same thread count and tier.
     *
     * A candidate with no runnable quality reference reports
     * [ThresholdOutcome.UNVERIFIABLE] rather than being passed: the plan's rule
     * has no referent without the larger model, and "no comparison available"
     * reported as "no regression" is precisely the substitution this map forbids.
     */
    private fun werRegressionCheck(cells: List<CellResult>): ThresholdCheck {
        val threshold = "regression vs the larger model in family ≤ +${Thresholds.WER_REGRESSION_MAX_ABSOLUTE_POINTS} points, absolute"
        val verdicts = mutableListOf<String>()

        for (candidate in ModelMatrix.PLANNED) {
            val referenceId = candidate.qualityReferenceId ?: continue
            val smaller = cells.filter { it.candidate.id == candidate.id }.firstOrNull()?.errorRate
            val larger = cells.filter { it.candidate.id == referenceId }.firstOrNull()?.errorRate
            if (smaller == null || larger == null) {
                verdicts += "${candidate.displayName}: unverifiable (needs $referenceId in the same run)"
                continue
            }
            val delta = smaller.errorPercent - larger.errorPercent
            verdicts += "${candidate.displayName}: ${signed(round2(delta))} pts vs $referenceId"
        }

        val failures = verdicts.filter { it.startsWith("unverifiable") }.size
        val over = verdicts.count { verdict ->
            val delta = Regex("([+-][0-9.]+) pts").find(verdict)?.groupValues?.get(1)?.toDoubleOrNull()
            delta != null && delta > Thresholds.WER_REGRESSION_MAX_ABSOLUTE_POINTS
        }

        val outcome = when {
            verdicts.isEmpty() -> ThresholdOutcome.UNVERIFIABLE
            over > 0 -> ThresholdOutcome.NOT_MET
            failures > 0 -> ThresholdOutcome.UNVERIFIABLE
            else -> ThresholdOutcome.MET
        }

        return ThresholdCheck(
            name = "WER regression",
            threshold = threshold,
            measured = if (verdicts.isEmpty()) "no comparable pair" else "${verdicts.size - failures} compared, $over over",
            outcome = outcome,
            note = if (verdicts.isEmpty()) {
                "No candidate in this run has a larger sibling in the matrix, so the rule has nothing to compare."
            } else {
                verdicts.joinToString("; ")
            },
        )
    }

    private fun statement(status: GateStatus, tiers: Set<DeviceTier>, devices: List<String>): String {
        val where = devices.ifEmpty { listOf("an unnamed device") }.joinToString(", ")
        val tierList = tiers.map { it.manifestValue }.sorted().joinToString(", ").ifEmpty { "none" }
        return if (status == GateStatus.GATE_RESULT) {
            "Gate result. Covered the ${Thresholds.REQUIRED_TIER.manifestValue} tier on $where " +
                "(tiers covered: $tierList)."
        } else {
            "INDICATIVE ONLY — not a gate result. Run on $where, tier(s) $tierList. The RTF bar is written " +
                "against the ${Thresholds.REQUIRED_TIER.manifestValue} tier, which this run did not cover. " +
                "model-selection.md keeps that bar exactly as written and unmeasured rather than moving it to " +
                "fit the hardware in hand."
        }
    }

    private fun round2(v: Double): String = "%.2f".format(v)
    private fun round3(v: Double): String = "%.3f".format(v)
    private fun signed(v: String): String = if (v.startsWith("-")) v else "+$v"
}
