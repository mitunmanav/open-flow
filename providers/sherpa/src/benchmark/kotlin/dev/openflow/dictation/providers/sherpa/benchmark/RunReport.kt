package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * One model, at one thread count, on one device tier: everything measured about
 * it.
 *
 * A "cell" is the unit the plan's matrix is written in — model × threads × device
 * — so it is the unit this is shaped like. Every metric is nullable and a null
 * means **not measured**, never zero: an offline family has no first partial
 * because it does not emit partials, which is a fact about the family, and
 * reporting it as `0 ms` would put a number in the table that no iteration
 * produced.
 *
 * [utterancesScored] against [utterancesTotal] is here for the same reason. WER
 * over a subset of the eval set is arithmetically valid and completely
 * misleading, so the denominator travels with the numerator.
 */
data class CellResult(
    val candidate: ModelCandidate,
    val numThreads: Int,
    val deviceTier: DeviceTier,
    val scoringStrategy: ScoringStrategy,
    val rtf: Summary?,
    val firstPartialMs: Double?,
    val partialIntervalMs: Double?,
    /** Endpoint or VAD-segment close → Final available. The origin the plan's ~1 s bar names. */
    val endpointToFinalMs: Double?,
    /** End of the user's speech → Final available. The glossary's Final Latency, and the number a user feels. */
    val speechEndToFinalMs: Double?,
    val errorRate: ErrorRate?,
    val peakRssBytes: Long?,
    val peakPssKb: Long?,
    val modelLoadMs: Double?,
    val batteryPercentPerMinute: Double?,
    val utterancesScored: Int,
    val utterancesTotal: Int,
    val thermalBefore: String,
    val thermalAfter: String,
    val notes: List<String> = emptyList(),
) {
    /** How a cell is named in the report. Stable, so two runs can be compared by eye. */
    val label: String get() = "${candidate.displayName} @ ${numThreads}t"

    /** True when every utterance in the eval set was scored. */
    val scoredWholeEvalSet: Boolean get() = utterancesScored == utterancesTotal
}

/**
 * A whole benchmark run, and the two renderings it produces.
 *
 * **[instrument] is not decoration.** `model-selection.md` settles that three
 * instruments report into `model-benchmark-results.md` as separate sections —
 * sherpa's own demo APKs, this API-level harness, and OpenFlow's `SpeechProvider`
 * — and that the gap between the second and third *is* the cost of the
 * integration. A number that does not carry its instrument is therefore not a
 * result, it is a rumour, and the field is non-optional and [INSTRUMENT] is fixed
 * rather than passed in so no caller can relabel this run as a provider one.
 */
data class RunReport(
    val device: DeviceRecord,
    val cpuCount: Int,
    val requestedThreads: List<Int>,
    val protocol: Protocol,
    val evalSet: EvalSet,
    val cells: List<CellResult>,
    /** Matrix candidates present in the plan but absent from this run, by id. */
    val candidatesNotMeasured: List<String>,
    /** Cells that were requested and could not run, each with the reason. */
    val cellsSkipped: List<String>,
    val verdict: ThresholdVerdict,
    val sherpaVersion: String,
    val generatedAtEpochMs: Long,
) {
    val instrument: String get() = INSTRUMENT

    companion object {
        /**
         * The label this instrument reports under, worded to match
         * `model-selection.md`'s own numbering so the results document can label
         * its sections by copying this string.
         */
        const val INSTRUMENT =
            "API-level harness (instrument 2 of 3): sherpa-onnx's Kotlin API directly — " +
                "OnlineRecognizer/OfflineRecognizer + Vad — over a fixed eval set. Directional. " +
                "Does NOT measure OpenFlow: no SpeechProvider, no decode thread, no endpoint→insertion."

        fun of(
            device: DeviceRecord,
            cpuCount: Int,
            requestedThreads: List<Int>,
            protocol: Protocol,
            evalSet: EvalSet,
            cells: List<CellResult>,
            verdict: ThresholdVerdict,
            sherpaVersion: String,
            generatedAtEpochMs: Long,
            candidatesNotMeasured: List<String> = emptyList(),
            cellsSkipped: List<String> = emptyList(),
        ): RunReport = RunReport(
            device = device,
            cpuCount = cpuCount,
            requestedThreads = requestedThreads,
            protocol = protocol,
            evalSet = evalSet,
            cells = cells,
            candidatesNotMeasured = candidatesNotMeasured,
            cellsSkipped = cellsSkipped,
            verdict = verdict,
            sherpaVersion = sherpaVersion,
            generatedAtEpochMs = generatedAtEpochMs,
        )

        /**
         * JSON string literal with the escapes the spec requires, and every
         * control character escaped rather than emitted raw.
         *
         * Model display names, OEM skins and device notes all reach here as free
         * text typed by a person into a manifest, and a raw newline inside a JSON
         * string makes the whole document unparseable — so a run would be lost to
         * a formatting slip in a note field. Unit-tested rather than trusted, for
         * that reason.
         */
        fun quote(raw: String): String = buildString {
            append('"')
            for (ch in raw) {
                when {
                    ch == '"' -> append("\\\"")
                    ch == '\\' -> append("\\\\")
                    ch == '\n' -> append("\\n")
                    ch == '\r' -> append("\\r")
                    ch == '\t' -> append("\\t")
                    ch == '\b' -> append("\\b")
                    ch == '\u000C' -> append("\\f")
                    ch < ' ' -> append("\\u%04x".format(ch.code))
                    else -> append(ch)
                }
            }
            append('"')
        }

        /** Fixed-decimal formatting, so a report does not vary with the platform locale. */
        fun round(value: Double, decimals: Int): String = "%1$.${decimals}f".format(value)

        fun numberJson(name: String, value: Double?, decimals: Int): String =
            if (value == null) "      \"$name\": null" else "      \"$name\": ${round(value, decimals)}"

        fun summaryJson(name: String, summary: Summary?, decimals: Int): String {
            if (summary == null) return "      \"$name\": null"
            return "      \"$name\": {\"n\": ${summary.n}, \"min\": ${round(summary.min, decimals)}, " +
                "\"median\": ${round(summary.median, decimals)}, \"p90\": ${round(summary.p90, decimals)}, " +
                "\"max\": ${round(summary.max, decimals)}, \"mean\": ${round(summary.mean, decimals)}}"
        }
    }

    /**
     * The machine-readable rendering.
     *
     * Hand-written rather than produced by a serialization library, because the
     * project has no JSON dependency and adding one for a report this size would
     * put a library in the shipped dependency graph to format eleven fields.
     * The cost is that this has to escape properly, so it does — and the escaping
     * is unit-tested rather than trusted, since a report that is malformed on the
     * one run that matters is a silent loss of the whole run.
     */
    fun toJson(): String = buildString {
        appendLine("{")
        appendLine("""  "instrument": ${quote(instrument)},""")
        appendLine("""  "sherpaVersion": ${quote(sherpaVersion)},""")
        appendLine("""  "generatedAtEpochMs": $generatedAtEpochMs,""")
        appendLine("""  "device": {""")
        appendLine("""    "name": ${quote(device.name)},""")
        appendLine("""    "tier": ${quote(device.tier.manifestValue)},""")
        appendLine("""    "abi": ${quote(device.abi)},""")
        appendLine("""    "oemSkin": ${quote(device.oemSkin)},""")
        appendLine("""    "androidMajor": ${device.androidMajor},""")
        appendLine("""    "cpuCount": $cpuCount,""")
        appendLine("""    "requestedThreads": [${requestedThreads.joinToString(", ")}],""")
        if (device.notes.isNotBlank()) appendLine("""    "notes": ${quote(device.notes)}""")
        appendLine("  },")
        appendLine("""  "protocol": {""")
        appendLine("""    "warmupIterations": ${protocol.warmupIterations},""")
        appendLine("""    "measuredIterations": ${protocol.measuredIterations},""")
        appendLine("""    "chunkMs": ${protocol.chunkMillis},""")
        appendLine("""    "trailingSilenceMs": ${protocol.trailingSilenceMillis},""")
        appendLine("""    "executionProvider": ${quote(protocol.executionProvider)}""")
        appendLine("  },")
        appendLine("""  "evalSet": {""")
        appendLine("""    "dir": ${quote(evalSet.dir)},""")
        appendLine("""    "pattern": ${quote(evalSet.pattern)},""")
        appendLine("""    "unit": ${quote(evalSet.unit.name)},""")
        appendLine("""    "referenceLayout": ${quote(evalSet.reference.manifestValue)}""")
        evalSet.transcriptPath?.let { appendLine(""",    "transcript": ${quote(it)}""") }
        appendLine("  },")
        appendLine("""  "verdict": {""")
        appendLine("""    "status": ${quote(verdict.status.name)},""")
        appendLine("""    "statement": ${quote(verdict.statement)},""")
        appendLine("""    "tiersCovered": [${verdict.tiersCovered.map { quote(it.manifestValue) }.sorted().joinToString(", ")}],""")
        appendLine("""    "thresholdsSource": ${quote(Thresholds.SOURCE)},""")
        appendLine("""    "checks": [""")
        verdict.checks.forEachIndexed { index, check ->
            appendLine("      {")
            appendLine("""        "name": ${quote(check.name)},""")
            appendLine("""        "threshold": ${quote(check.threshold)},""")
            appendLine("""        "measured": ${quote(check.measured)},""")
            appendLine("""        "outcome": ${quote(check.outcome.name)}""")
            if (check.note.isNotBlank()) appendLine(""",        "note": ${quote(check.note)}""")
            appendLine(if (index == verdict.checks.lastIndex) "      }" else "      },")
        }
        appendLine("    ]")
        appendLine("  },")
        appendLine("""  "cells": [""")
        cells.forEachIndexed { index, cell ->
            appendLine("    {")
            appendLine("""      "model": ${quote(cell.candidate.id)},""")
            appendLine("""      "displayName": ${quote(cell.candidate.displayName)},""")
            appendLine("""      "family": ${quote(cell.candidate.family.name)},""")
            appendLine("""      "scoringStrategy": ${quote(cell.scoringStrategy.name)},""")
            appendLine("""      "numThreads": ${cell.numThreads},""")
            appendLine("""      "deviceTier": ${quote(cell.deviceTier.manifestValue)},""")
            appendLine(summaryJson("rtf", cell.rtf, 4) + ",")
            appendLine(numberJson("firstPartialMs", cell.firstPartialMs, 1) + ",")
            appendLine(numberJson("partialIntervalMs", cell.partialIntervalMs, 1) + ",")
            appendLine(numberJson("endpointToFinalMs", cell.endpointToFinalMs, 1) + ",")
            appendLine(numberJson("speechEndToFinalMs", cell.speechEndToFinalMs, 1) + ",")
            appendLine(
                if (cell.errorRate == null) {
                    "      \"errorRate\": null,"
                } else {
                    "      \"errorRate\": {\"unit\": ${quote(cell.errorRate.unit.label)}, \"referenceUnits\": " +
                        "${cell.errorRate.referenceUnits}, \"substitutions\": ${cell.errorRate.substitutions}, " +
                        "\"deletions\": ${cell.errorRate.deletions}, \"insertions\": ${cell.errorRate.insertions}, " +
                        "\"errorPercent\": ${round(cell.errorRate.errorPercent, 2)}},"
                }
            )
            appendLine(numberJson("peakRssBytes", cell.peakRssBytes?.toDouble(), 0) + ",")
            appendLine(numberJson("peakPssKb", cell.peakPssKb?.toDouble(), 0) + ",")
            appendLine(numberJson("modelLoadMs", cell.modelLoadMs, 1) + ",")
            appendLine(numberJson("batteryPercentPerMinute", cell.batteryPercentPerMinute, 3) + ",")
            appendLine("""      "utterancesScored": ${cell.utterancesScored},""")
            appendLine("""      "utterancesTotal": ${cell.utterancesTotal},""")
            appendLine("""      "thermalBefore": ${quote(cell.thermalBefore)},""")
            appendLine("""      "thermalAfter": ${quote(cell.thermalAfter)}""")
            if (cell.notes.isNotEmpty()) {
                appendLine(",      \"notes\": [${cell.notes.joinToString(", ") { quote(it) }}]")
            }
            appendLine(if (index == cells.lastIndex) "    }" else "    },")
        }
        appendLine("  ],")
        // What the run did NOT do, in the machine-readable half as well as the
        // prose one. A report whose omissions live only in a paragraph is a report
        // that reads as complete when parsed.
        appendLine("""  "candidatesNotMeasured": [${candidatesNotMeasured.joinToString(", ") { quote(it) }}],""")
        appendLine("""  "cellsSkipped": [${cellsSkipped.joinToString(", ") { quote(it) }}]""")
        appendLine("}")
    }

    /**
     * The human rendering, for pasting into `model-benchmark-results.md`.
     *
     * Carries the instrument label and the verdict statement **above** the table,
     * not in a footnote. A table lifted out of this report without its two header
     * lines is a set of numbers with no producer and no standing, which is
     * exactly what that document exists to prevent.
     */
    fun toMarkdown(): String = buildString {
        appendLine("### $instrument")
        appendLine()
        appendLine("**${verdict.statement}**")
        appendLine()
        appendLine(
            "| device | tier | ABI | Android | sherpa-onnx | eval set | unit | threads requested / cores |"
        )
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- |")
        appendLine(
            "| ${device.name} | ${device.tier.manifestValue} | ${device.abi} | ${device.androidMajor} " +
                "| $sherpaVersion | `${evalSet.dir}` | ${evalSet.unit.label} | " +
                "${requestedThreads.joinToString(", ")} / $cpuCount |"
        )
        appendLine()
        appendLine(
            "Protocol: ${protocol.warmupIterations} warmup decodes discarded, median of " +
                "${protocol.measuredIterations} iterations, ${protocol.chunkMillis} ms chunks, " +
                "${protocol.trailingSilenceMillis} ms trailing silence, provider=\"${protocol.executionProvider}\"."
        )
        appendLine()
        appendLine("| model | threads | how scored | RTF median | 1st partial | partial every | endpoint→Final | speech-end→Final | score | peak RSS | load |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        for (cell in cells) {
            appendLine(
                "| ${cell.candidate.displayName} | ${cell.numThreads} | ${cell.scoringStrategy} " +
                    "| ${cell.rtf?.median?.let { round(it, 3) } ?: "—"} " +
                    "| ${cell.firstPartialMs?.let { "${it.toLong()} ms" } ?: "n/a"} " +
                    "| ${cell.partialIntervalMs?.let { "${it.toLong()} ms" } ?: "n/a"} " +
                    "| ${cell.endpointToFinalMs?.let { "${it.toLong()} ms" } ?: "—"} " +
                    "| ${cell.speechEndToFinalMs?.let { "${it.toLong()} ms" } ?: "—"} " +
                    "| ${cell.errorRate?.let { "${round(it.errorPercent, 1)}% ${it.unit.label}" } ?: "—"} " +
                    "| ${cell.peakRssBytes?.let { "${it / 1_048_576} MB" } ?: "—"} " +
                    "| ${cell.modelLoadMs?.let { "${it.toLong()} ms" } ?: "—"} |"
            )
        }
        appendLine()
        if (utteranceCoverageIncomplete()) {
            appendLine(
                "> **Incomplete WER coverage.** At least one cell scored fewer utterances than the eval set " +
                    "holds. Those scores are over a subset and are not comparable to a whole-corpus figure."
            )
            appendLine()
        }
        appendLine("| threshold | bound | measured | outcome |")
        appendLine("| --- | --- | --- | --- |")
        for (check in verdict.checks) {
            appendLine("| ${check.name} | ${check.threshold} | ${check.measured} | ${check.outcome.name} |")
        }
        appendLine()
        appendLine("RTF is a benchmark-only quantity and is never computed at runtime — see `GLOSSARY.md`.")
        if (candidatesNotMeasured.isNotEmpty()) {
            appendLine()
            appendLine(
                "**Not measured in this run:** ${candidatesNotMeasured.joinToString(", ") { "`$it`" }}. " +
                    "These are candidates in `model-selection.md`'s matrix that this run did not cover; the table " +
                    "above is not a result for them."
            )
        }
        if (cellsSkipped.isNotEmpty()) {
            appendLine()
            appendLine("**Skipped:**")
            for (reason in cellsSkipped) appendLine("- $reason")
        }
    }

    private fun utteranceCoverageIncomplete(): Boolean = cells.any { !it.scoredWholeEvalSet }
}
