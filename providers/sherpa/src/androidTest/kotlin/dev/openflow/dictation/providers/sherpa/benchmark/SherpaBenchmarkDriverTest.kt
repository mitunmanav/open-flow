package dev.openflow.dictation.providers.sherpa.benchmark

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The benchmark's entry point, and the only thing in this project that calls sherpa-onnx on a
 * device.
 *
 * **It is an instrumentation test because that is the only way to get a native ONNX runtime onto
 * a phone without shipping an APK.** The consequences are stated rather than worked around:
 *
 * - It runs under `connectedDebugAndroidTest`, and it **skips** when no `-e manifest` argument is
 *   supplied. The scheduled emulator job in `.github/workflows/android-test.yml` therefore stays
 *   green, and a skip is visible in the report as a skip rather than as a pass.
 * - A full matrix cell is 20+ iterations over the whole eval set. The run is bounded by the
 *   manifest: name one model, not eight, per invocation, and `candidatesNotMeasured` records the
 *   rest. The procedure is in `docs/providers/model-benchmark-harness.md`.
 *
 * **What it does not do: decide anything.** It measures, writes a report whose verdict names the
 * device and its tier, and stops. `model-selection.md`'s *Decision rule* is ticket 41's output,
 * and the RTF bar's referent — a weakest device tier — does not exist in this project yet.
 *
 * ## Instrumentation arguments
 *
 * | Key | Required | Meaning |
 * | --- | --- | --- |
 * | `manifest` | yes | Device path to the run manifest. Its absence skips the test. |
 * | `out` | no | Directory for the report. Defaults to the test APK's external files dir. |
 * | `smoke_iterations` | no | Overrides `measured_iterations` **below** the plan's floor of 20, for a shakedown. The reduced count is written into the report's protocol block, so a smoke run cannot be mistaken for a protocol run. |
 */
@RunWith(AndroidJUnit4::class)
class SherpaBenchmarkDriverTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.context

    /**
     * The `-e key value` pairs the run was invoked with.
     *
     * `InstrumentationRegistry.getArguments()` rather than `Instrumentation.getArguments()`:
     * the latter is not on the compile-time `android.jar` this module builds against, so the
     * static accessor is the one that resolves.
     */
    private val arguments get() = InstrumentationRegistry.getArguments()
    private val telemetry by lazy { DeviceTelemetry(context) }

    @Test
    fun runTheManifest() {
        val args = arguments
        val manifestPath = args.getString("manifest")
        assumeTrue(
            "No -e manifest was supplied, so there is no eval set, no models and nothing to measure. " +
                "The scheduled emulator run in .github/workflows/android-test.yml invokes this without " +
                "arguments and is expected to skip. The procedure for a real run is in " +
                "docs/providers/model-benchmark-harness.md.",
            !manifestPath.isNullOrBlank(),
        )

        val manifestFile = File(requireNotNull(manifestPath))
        assertTrue("The manifest '$manifestPath' is not on this device.", manifestFile.isFile)

        val manifest = manifestFile.reader(Charsets.UTF_8).use { BenchmarkManifestReader.parse(it) }
        // The whole run's protocol in one line, in the log before anything is measured, so a
        // report that turns up in a pull request can be matched to the invocation that made it.
        log(
            "PROTOCOL device=${telemetry.describe()} tier=${manifest.device.tier.manifestValue} " +
                "abi=${manifest.device.abi} cores=${telemetry.cpuCount} threads=${manifest.protocol.threadCounts} " +
                "warmup=${manifest.protocol.warmupIterations} measured=${manifest.protocol.measuredIterations} " +
                "chunk=${manifest.protocol.chunkMillis}ms trailing=${manifest.protocol.trailingSilenceMillis}ms " +
                "provider=${manifest.protocol.executionProvider} sherpa=${DeviceTelemetry.sherpaVersion()}"
        )

        val smokeIterations = args.getString("smoke_iterations")?.toIntOrNull()?.takeIf { it > 0 }
        val protocol = smokeIterations
            ?.let { manifest.protocol.copy(measuredIterations = it) }
            ?: manifest.protocol
        if (smokeIterations != null) {
            log(
                "WARNING smoke_iterations=$smokeIterations overrides the plan's floor of " +
                    "${BenchmarkManifestReader.MIN_MEASURED_ITERATIONS}. This is a shakedown run, not a " +
                    "measurement, and the report says so in its protocol block."
            )
        }

        val evalSet = manifest.evalSet
        val utterances = EvalSetCatalog(
            directory = File(evalSet.dir),
            pattern = evalSet.pattern,
            referenceLayout = evalSet.reference,
            transcript = evalSet.transcriptPath?.let(::File),
            unit = evalSet.unit,
        ).utterances()
        // Read once, outside every timing loop: a WAV read inside the measured pass would put
        // this harness's allocation in the RTF numerator.
        val audio = utterances.map { it to WavReader.read(it.audio) }
        val totalAudioMillis = audio.sumOf { it.second.size * 1000.0 / SherpaAsr.SAMPLE_RATE }
        log("EVAL SET ${utterances.size} utterance(s), ${totalAudioMillis / 1000.0} s total, unit=${evalSet.unit.label}")

        val skipped = mutableListOf<String>()
        val cells = mutableListOf<CellResult>()
        val measuredIds = mutableSetOf<String>()

        for (entry in manifest.models) {
            val missing = SherpaAsr.missingFiles(entry)
            if (missing.isNotEmpty()) {
                // Reported, not thrown. A person staging eight models by hand will get one wrong,
                // and losing the other seven to it would be worse than a report that says which
                // cell was skipped and which file was absent.
                skipped += "${entry.candidate.displayName}: not on this device — ${missing.joinToString(", ")}"
                log("SKIP ${entry.candidate.displayName}: missing ${missing.joinToString(", ")}")
                continue
            }
            for (numThreads in protocol.threadCounts) {
                val cell = runCell(entry, numThreads, protocol, evalSet.unit, audio, totalAudioMillis, manifest)
                if (cell != null) {
                    cells += cell
                    measuredIds += entry.candidate.id
                }
            }
        }

        val battery = runBatteryPhase(manifest, protocol, cells, audio, totalAudioMillis)
        val cellsWithBattery = attachBattery(cells, battery)

        val notMeasured = ModelMatrix.PLANNED.map { it.id }.filterNot { it in measuredIds }
        val verdict = ThresholdVerdictEvaluator.evaluate(cellsWithBattery, listOf(manifest.device.name))
        val report = RunReport.of(
            device = manifest.device,
            cpuCount = telemetry.cpuCount,
            requestedThreads = protocol.threadCounts,
            protocol = protocol,
            evalSet = evalSet,
            cells = cellsWithBattery,
            verdict = verdict,
            sherpaVersion = DeviceTelemetry.sherpaVersion(),
            generatedAtEpochMs = System.currentTimeMillis(),
            candidatesNotMeasured = notMeasured,
            cellsSkipped = skipped,
        )

        writeReport(report, args.getString("out"))
        log("VERDICT ${verdict.status}: ${verdict.statement}")
        assertTrue(
            "The run produced no cells, so nothing was measured. Check the skipped list above and the " +
                "manifest's model directories: ${skipped.joinToString("; ")}",
            cellsWithBattery.isNotEmpty(),
        )
    }

    // ---- one cell --------------------------------------------------------------------

    /**
     * One model at one thread count: load it, warm it up, measure it.
     *
     * The recognizer is constructed **once** and both timed and used, so `modelLoadMs` is the
     * load the measurement actually ran against rather than a separate load that happened to cost
     * the same. The VAD is built separately for offline families and is not timed, because the
     * plan's "model load time" metric names the ASR model.
     */
    private fun runCell(
        entry: ModelEntry,
        numThreads: Int,
        protocol: Protocol,
        unit: ScoreUnit,
        audio: List<Pair<Utterance, FloatArray>>,
        totalAudioMillis: Double,
        manifest: BenchmarkManifest,
    ): CellResult? {
        val label = "${entry.candidate.displayName} @ ${numThreads}t"
        val online = entry.candidate.family.isStreaming
        val thermal = telemetry.startThermalWatch()
        val notes = mutableListOf<String>()
        val rtfs = mutableListOf<Double>()
        val firstPartials = mutableListOf<Double>()
        val partialIntervals = mutableListOf<Double>()
        val endpointToFinal = mutableListOf<Double>()
        val speechEndToFinal = mutableListOf<Double>()
        var peakRss = 0L
        var peakPss = 0L
        var scored = 0
        var total = audio.size
        var corpusRate: ErrorRate? = null

        var recognizerLoadMs: Double? = null
        var asr: OnlineAsr? = null
        var offlineAsr: OfflineAsr? = null
        var vad: VadSegmenter? = null

        try {
            val loadStart = System.nanoTime()
            if (online) {
                asr = SherpaAsr.online(entry, numThreads, assetManager = null)
            } else {
                offlineAsr = SherpaAsr.offline(entry, numThreads, assetManager = null)
                vad = SherpaAsr.vad(manifest.vad, numThreads, assetManager = null)
            }
            recognizerLoadMs = (System.nanoTime() - loadStart) / 1_000_000.0

            val totalIterations = protocol.warmupIterations + protocol.measuredIterations
            for (iteration in 0 until totalIterations) {
                val measured = iteration >= protocol.warmupIterations
                val perIteration = oneIteration(
                    entry = entry,
                    asr = asr,
                    offlineAsr = offlineAsr,
                    vad = vad,
                    audio = audio,
                    protocol = protocol,
                    unit = unit,
                    totalAudioMillis = totalAudioMillis,
                    collect = measured,
                    scoreThisIteration = measured && corpusRate == null,
                )
                peakRss = maxOf(peakRss, telemetry.residentSetSizeBytes() ?: 0L)
                peakPss = maxOf(peakPss, telemetry.totalPssKb() ?: 0L)
                if (!measured) continue

                rtfs += perIteration.rtf
                perIteration.firstPartialMs?.let { firstPartials += it }
                perIteration.partialIntervalMs?.let { partialIntervals += it }
                perIteration.endpointToFinalMs?.let { endpointToFinal += it }
                perIteration.speechEndToFinalMs?.let { speechEndToFinal += it }
                if (perIteration.transcripts.isNotEmpty()) {
                    // Greedy search is deterministic, so one iteration's text is the run's text.
                    // Scoring it again for 20 iterations would cost 20 decodes to learn nothing.
                    corpusRate = perIteration.errorRate
                    scored = perIteration.scored
                    total = perIteration.total
                }
                if ((iteration - protocol.warmupIterations + 1) % 5 == 0) {
                    log("  $label iteration ${iteration - protocol.warmupIterations + 1}/${protocol.measuredIterations} rtf=${RunReport.round(perIteration.rtf, 3)}")
                }
            }
            if (corpusRate == null) {
                notes += "No iteration produced a scoreable transcript, so the error-rate column is empty. " +
                    "Every utterance is listed below."
                for ((utterance, _) in audio) {
                    notes += "no final for ${utterance.id}"
                }
            }
            notes += "peak RSS read from /proc/self/statm with a 4096 B page size; PSS is the page-size-free figure"
            notes += "device reported: ${telemetry.describe()}"
        } finally {
            asr?.release()
            offlineAsr?.release()
            vad?.release()
        }

        val (maxThermal, thermalAfter) = thermal.finish()
        thermal.release()
        val medianRtf = Summary.of(rtfs)?.median
        log("CELL $label rtf=${medianRtf?.let { RunReport.round(it, 3) } ?: "none"} thermal=$maxThermal→$thermalAfter")

        return CellResult(
            candidate = entry.candidate,
            numThreads = numThreads,
            deviceTier = manifest.device.tier,
            scoringStrategy = entry.candidate.family.scoringStrategy,
            rtf = Summary.of(rtfs),
            // Per iteration the utterances are reduced by their median, and the iterations by
            // theirs again: a mean across utterances would let one thirty-second recording
            // outvote nineteen five-second ones.
            firstPartialMs = Summary.of(firstPartials)?.median,
            partialIntervalMs = Summary.of(partialIntervals)?.median,
            endpointToFinalMs = Summary.of(endpointToFinal)?.median,
            speechEndToFinalMs = Summary.of(speechEndToFinal)?.median,
            errorRate = corpusRate,
            peakRssBytes = peakRss.takeIf { it > 0 },
            peakPssKb = peakPss.takeIf { it > 0 },
            modelLoadMs = recognizerLoadMs,
            batteryPercentPerMinute = null,
            utterancesScored = scored,
            utterancesTotal = total,
            thermalBefore = thermal.before,
            thermalAfter = maxThermal + "→" + thermalAfter,
            notes = notes,
        )
    }

    /** One pass over the whole eval set. */
    private class IterationResult(
        val rtf: Double,
        val firstPartialMs: Double?,
        val partialIntervalMs: Double?,
        val endpointToFinalMs: Double?,
        val speechEndToFinalMs: Double?,
        val transcripts: List<Pair<String, String>>,
        val errorRate: ErrorRate?,
        val scored: Int,
        val total: Int,
    )

    /**
     * A whole pass over the eval set, which is what one "iteration" means.
     *
     * `model-selection.md` says "median of ≥20 iterations" without saying over what; a full pass
     * over the fixed set is the reading that makes RTF a property of the corpus rather than of
     * one lucky clip, and it is what a dictation session of that corpus's length would cost.
     */
    private fun oneIteration(
        entry: ModelEntry,
        asr: OnlineAsr?,
        offlineAsr: OfflineAsr?,
        vad: VadSegmenter?,
        audio: List<Pair<Utterance, FloatArray>>,
        protocol: Protocol,
        unit: ScoreUnit,
        totalAudioMillis: Double,
        collect: Boolean,
        scoreThisIteration: Boolean,
    ): IterationResult {
        val online = entry.candidate.family.isStreaming
        val firstPartials = mutableListOf<Double>()
        val partialIntervals = mutableListOf<Double>()
        val endpointToFinal = mutableListOf<Double>()
        val speechEndToFinal = mutableListOf<Double>()
        val transcripts = mutableListOf<Pair<String, String>>()
        var decodeMillis = 0.0

        if (online) {
            val session = requireNotNull(asr).createSession()
            try {
                for ((utterance, samples) in audio) {
                    val result = measureOnlinePass(
                        session = session,
                        samples = samples,
                        sampleRate = SherpaAsr.SAMPLE_RATE,
                        chunkMillis = protocol.chunkMillis,
                        trailingSilenceMillis = protocol.trailingSilenceMillis,
                    )
                    decodeMillis += result.decodeMillis
                    if (collect) {
                        result.firstPartialMs?.let { firstPartials += it }
                        result.partialIntervalMs?.let { partialIntervals += it }
                        result.speechEndToFinalMs?.let { speechEndToFinal += it }
                    }
                    // Only the endpoint result is ever scored. The trailing partial is the
                    // recognizer's best guess so far; scoring it here is the whole-file-decode
                    // mistake wearing a streaming label.
                    val text = result.finalText
                    if (scoreThisIteration && text != null) transcripts += text to utterance.reference
                    session.reset()
                }
            } finally {
                session.release()
            }
        } else {
            requireNotNull(vad)
            requireNotNull(offlineAsr)
            for ((utterance, samples) in audio) {
                val result = measureOfflinePass(
                    vad = vad,
                    asr = offlineAsr,
                    samples = samples,
                    sampleRate = SherpaAsr.SAMPLE_RATE,
                    chunkMillis = protocol.chunkMillis,
                )
                decodeMillis += result.passMillis
                if (collect) {
                    result.endpointToFinalMs?.let { endpointToFinal += it }
                    result.speechEndToFinalMs?.let { speechEndToFinal += it }
                }
                if (scoreThisIteration) {
                    // One transcript per utterance: the segments are joined, because the eval set's
                    // reference is one line per file and a split reference is not a reference.
                    val joined = result.segmentTexts.filter { it.isNotBlank() }.joinToString(" ")
                    transcripts += joined to utterance.reference
                }
            }
        }

        // Scored only when this iteration was the scoring one, so `scored`/`total` describe the
        // corpus the error rate was computed over rather than a pass that collected nothing.
        val perUtterance = transcripts.map { (hypothesis, reference) ->
            WordErrorRate.score(hypothesis, reference, unit)
        }
        return IterationResult(
            rtf = decodeMillis / totalAudioMillis,
            firstPartialMs = Summary.of(firstPartials)?.median,
            partialIntervalMs = Summary.of(partialIntervals)?.median,
            endpointToFinalMs = Summary.of(endpointToFinal)?.median,
            speechEndToFinalMs = Summary.of(speechEndToFinal)?.median,
            transcripts = transcripts,
            // `aggregate` is null when any utterance could not be scored rather than dropping it,
            // so a subset never masquerades as a whole-corpus figure.
            errorRate = WordErrorRate.aggregate(perUtterance.filterNotNull()),
            scored = perUtterance.count { it != null },
            total = transcripts.size,
        )
    }

    // ---- the ten-minute session -----------------------------------------------------

    /**
     * Battery drain over a continuous session, on one model.
     *
     * The plan says "one device, one battery-tier model", so this runs on the first cell that
     * measured rather than once per cell: ten minutes per cell would be eighty minutes of
     * nothing but battery, and the plan's own arithmetic is one device and one model.
     *
     * Nothing here feeds the matrix. `ThresholdVerdict` reports the figure and asserts nothing
     * about it, because the plan sets no number — and a drain figure taken from a charging phone
     * is worse than none, which is why the plugged state travels with it in the cell's notes.
     */
    private fun runBatteryPhase(
        manifest: BenchmarkManifest,
        protocol: Protocol,
        cells: List<CellResult>,
        audio: List<Pair<Utterance, FloatArray>>,
        totalAudioMillis: Double,
    ): Double? {
        val minutes = manifest.batterySessionMinutes
        if (minutes < 1) return null
        val target = cells.firstOrNull() ?: return null
        val entry = manifest.models.firstOrNull { it.candidate.id == target.candidate.id }
        if (entry == null) {
            log("BATTERY skipped: no manifest entry for ${target.candidate.id}")
            return null
        }
        val before = telemetry.battery()
        log("BATTERY ${minutes} minute session on ${entry.candidate.displayName}; before=${before?.render() ?: "unreadable"}")
        val deadline = System.nanoTime() + minutes * 60L * 1_000_000_000L
        val asr = if (target.scoringStrategy == ScoringStrategy.CHUNKED_STREAMING) {
            SherpaAsr.online(entry, target.numThreads, assetManager = null)
        } else {
            null
        }
        val offline = if (asr == null) SherpaAsr.offline(entry, target.numThreads, assetManager = null) else null
        val vad = if (offline != null) SherpaAsr.vad(manifest.vad, target.numThreads, assetManager = null) else null
        try {
            while (System.nanoTime() < deadline) {
                oneIteration(
                    entry = entry,
                    asr = asr,
                    offlineAsr = offline,
                    vad = vad,
                    audio = audio,
                    protocol = protocol,
                    unit = ScoreUnit.WORD,
                    totalAudioMillis = totalAudioMillis,
                    collect = false,
                    scoreThisIteration = false,
                )
            }
        } finally {
            asr?.release()
            offline?.release()
            vad?.release()
        }
        val after = telemetry.battery()
        val drain = if (before == null || after == null) {
            log("BATTERY not measurable: before=${before?.render()} after=${after?.render()}")
            null
        } else {
            (before.percent - after.percent) / minutes
        }
        log("BATTERY done; after=${after?.render() ?: "unreadable"} drain=$drain %/min")
        return drain
    }

    /**
     * The drain figure on the one cell it was measured for, and on no other.
     *
     * Copied onto every cell of the same model and thread count, and on no other: a number
     * attached to a cell it was not measured for is a number nobody measured.
     */
    private fun attachBattery(cells: List<CellResult>, drain: Double?): List<CellResult> {
        if (drain == null || cells.isEmpty()) return cells
        val target = cells.first()
        return cells.map { cell ->
            if (cell.candidate.id == target.candidate.id && cell.numThreads == target.numThreads) {
                cell.copy(batteryPercentPerMinute = drain)
            } else {
                cell
            }
        }
    }

    // ---- output ----------------------------------------------------------------------

    /**
     * Write both renderings, and say loudly where they are.
     *
     * The default directory is the test APK's own external files dir, which needs no runtime
     * permission and which `adb pull` can reach. The absolute path is logged because a person
     * holding a phone should not have to guess a package name to collect the run.
     */
    private fun writeReport(report: RunReport, outDir: String?) {
        val dir = outDir?.let(::File)
            ?: context.getExternalFilesDir(null)
            ?: File(context.filesDir, "benchmark")
        dir.mkdirs()
        val stamp = report.generatedAtEpochMs
        val json = File(dir, "openflow-benchmark-$stamp.json")
        val markdown = File(dir, "openflow-benchmark-$stamp.md")
        json.writeText(report.toJson())
        markdown.writeText(report.toMarkdown())
        log("REPORT JSON ${json.absolutePath}")
        log("REPORT MARKDOWN ${markdown.absolutePath}")
        log("REPORT PREVIEW\n${report.toMarkdown().take(400)}")
    }

    private fun log(message: String) = Log.i(TAG, message)

    private companion object {
        const val TAG = "openflow-benchmark"
    }
}
