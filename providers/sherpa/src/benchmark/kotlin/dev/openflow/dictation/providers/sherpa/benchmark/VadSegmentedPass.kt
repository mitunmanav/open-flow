package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * What one VAD-segmented pass over one offline utterance measured.
 *
 * [endpointToFinalMs] is the plan's *"Final within ~1 s of endpoint/VAD-segment close"* and it
 * **includes the VAD segmentation cost** — see [measureOfflinePass] for what that means and why
 * it has to be added rather than hoped to appear in a decode timer.
 */
data class OfflinePassResult(
    /** One transcript per VAD segment, in order. A file the VAD found silent yields none. */
    val segmentTexts: List<String>,
    /** Segments the VAD closed. Zero is a legitimate outcome: a file with no speech. */
    val segmentCount: Int,
    /** Samples inside the segments that were decoded. RTF's denominator for this pass. */
    val scoredSamples: Int,
    /** Wall clock the VAD spent computing across the whole file. Small, but real, and counted. */
    val vadComputeMillis: Double,
    /** Wall clock spent in `OfflineRecognizer.decode` + `getResult` across every segment. */
    val decodeMillis: Double,
    /** Audio-time the VAD consumed past each segment's end before it would close it. */
    val vadWaitMillis: Double,
    /** [vadWaitMillis] + [vadComputeMillis] + [decodeMillis]. The plan's offline quantity. */
    val endpointToFinalMs: Double?,
    /** Equal to [endpointToFinalMs] here; the VAD's wait *is* the speech-end→endpoint latency. */
    val speechEndToFinalMs: Double?,
    /** Wall clock for the whole pass, VAD included. The RTF numerator. */
    val passMillis: Double,
    /** Samples accepted by the VAD, which is every sample of the file. */
    val fedSamples: Int,
)

/**
 * Drives one offline recognizer through one utterance as **VAD-segmented simulated streaming**.
 *
 * This is the `SherpaOnnxSimulateStreamingAsr` shape `docs/providers/sherpa-onnx.md` records:
 * the offline model is fed whole VAD segments rather than the file, so the number describes the
 * system a dictation user would actually experience — a bounded utterance, one Final per
 * utterance, no partials — rather than a 30-second single decode nobody ever waits for.
 *
 * **Why the segmentation cost is added explicitly.** Three clocks are running and only two of
 * them are wall clock:
 *
 * 1. the VAD's own compute, measured;
 * 2. the offline decode, measured;
 * 3. the time the VAD *waited* — `minSilenceDuration` of trailing audio after the last speech
 *    sample before it would call the segment closed.
 *
 * (3) is the trap. The pass feeds audio as fast as it decodes, so that wait costs no wall
 * clock at all and would be invisible to a stopwatch — while in a live dictation it is the
 * single largest term in how long the user waits for a Final. So it is measured in **audio
 * time** instead: [VadSegmenter.consumedSampleCount] is how many samples the VAD has been given,
 * and a segment that ends at sample `E` and surfaces when `C` samples have been consumed waited
 * exactly `C - E` samples. That is a measurement of what the VAD actually did, not a model of
 * what it should have done, which is why the manifest still has to record the VAD's settings:
 * they are part of what this number means.
 */
fun measureOfflinePass(
    vad: VadSegmenter,
    asr: OfflineAsr,
    samples: FloatArray,
    sampleRate: Int,
    chunkMillis: Int,
    clock: MonotonicClock = MonotonicClock.SYSTEM,
): OfflinePassResult {
    val passStart = clock.nanos()
    val chunkSamples = (chunkMillis * sampleRate) / 1000
    require(chunkSamples > 0) { "chunk_ms=$chunkMillis is under one sample at $sampleRate Hz" }

    val texts = mutableListOf<String>()
    var segmentCount = 0
    var scoredSamples = 0
    var vadComputeNanos = 0L
    var vadWaitSamples = 0
    var decodeNanos = 0L

    /**
     * Decode every segment the VAD currently holds, accounting for the wait it already spent.
     *
     * The VAD's own cost is timed around `vad.drain()` alone and the decode is timed separately,
     * so a decode that happens to be slow cannot inflate the segmentation term. Getting this
     * wrong is not cosmetic: `VadSegmentedPassTest.theVadWaitIsAccountedForEvenThoughItCostsNoWallClock`
     * measures the two terms against a scripted clock and fails if they are blended.
     */
    fun drain() {
        val drainStart = clock.nanos()
        val segments = vad.drain()
        vadComputeNanos += clock.nanos() - drainStart
        for (segment in segments) {
            segmentCount++
            scoredSamples += segment.samples.size
            // Samples the VAD consumed past this segment's end before surfacing it. Zero for a
            // segment closed by `flush()`, which is the point of `flush()` — the harness knows
            // the difference, and so does the report.
            vadWaitSamples += (vad.consumedSampleCount - segment.endSample).coerceAtLeast(0)
            val decodeStart = clock.nanos()
            texts += asr.decode(segment.samples, sampleRate)
            decodeNanos += clock.nanos() - decodeStart
        }
    }

    var offset = 0
    while (offset < samples.size) {
        val end = minOf(offset + chunkSamples, samples.size)
        val piece = samples.copyOfRange(offset, end)
        offset = end
        val acceptStart = clock.nanos()
        // One argument. `Vad.acceptWaveform(float[])` takes no sample rate — verified with
        // `javap` against the artifact, and the opposite of both stream classes. Passing two
        // here, copied from the neighbouring stream loop, would not compile; passing one to a
        // stream would.
        vad.acceptToVad(piece)
        vadComputeNanos += clock.nanos() - acceptStart
        drain()
    }

    // `flush()` forces out the tail segment a dictation would otherwise lose at end of stream.
    val flushStart = clock.nanos()
    vad.flush()
    vadComputeNanos += clock.nanos() - flushStart
    drain()

    val vadWaitMillis = vadWaitSamples * 1000.0 / sampleRate
    val vadComputeMillis = vadComputeNanos / 1_000_000.0
    val decodeMillis = decodeNanos / 1_000_000.0
    // Zero segments is a real outcome — a silent or pure-noise file — and there is no Final to
    // be late for, so the quantity is absent rather than zero.
    val endpointToFinal = if (segmentCount == 0) null else vadWaitMillis + vadComputeMillis + decodeMillis

    return OfflinePassResult(
        segmentTexts = texts,
        segmentCount = segmentCount,
        scoredSamples = scoredSamples,
        vadComputeMillis = vadComputeMillis,
        decodeMillis = decodeMillis,
        vadWaitMillis = vadWaitMillis,
        endpointToFinalMs = endpointToFinal,
        speechEndToFinalMs = endpointToFinal,
        passMillis = (clock.nanos() - passStart) / 1_000_000.0,
        fedSamples = vad.consumedSampleCount,
    )
}
