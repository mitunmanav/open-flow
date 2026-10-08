package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * What one chunked simulated streaming pass over one online utterance measured.
 *
 * Every duration is a **wall-clock** measurement except [firstPartialMs] and
 * [partialIntervalMs], which are in **audio time**. That split is deliberate:
 *
 * - A user feels *when* a partial appeared, and the threshold the plan states —
 *   "first partial ≤ 500 ms" — is about the recognizer having produced something within the
 *   first half second of the utterance. Measured in audio time that is a property of the
 *   model and is the same on a fast phone and a slow one. Measured in wall clock it would be
 *   audio time divided by RTF, so a slow device would report a *better* first partial purely
 *   by taking longer to get there, which is not a property of anything.
 * - Cadence is the same argument applied repeatedly.
 *
 * RTF's own numerator is wall clock, because that is what "faster than real time" means.
 */
data class OnlinePassResult(
    /** The endpoint result, when one was reached. `null` means no Final, and is not `""`. */
    val finalText: String?,
    val endpointReached: Boolean,
    /** Audio-time of the first partial that changed the text, ms from the first sample fed. */
    val firstPartialMs: Double?,
    /** Median audio-time gap between consecutive changed partials, ms. */
    val partialIntervalMs: Double?,
    /**
     * Always `null`, and deliberately so. See the function below: an online recognizer's
     * endpoint result *is* its Final, so this window is satisfied by construction and
     * reporting it would put a passing number in a column whose bound every online cell
     * clears automatically.
     */
    val endpointToFinalMs: Double?,
    /** Wall clock from the last sample going in to the Final text being in hand. Work, not waiting. */
    val speechEndToFinalMs: Double?,
    /** Wall clock for the whole pass: accept, decode and result fetch for every chunk. */
    val decodeMillis: Double,
    /** Samples fed to the recognizer, including any trailing silence this harness padded on. */
    val fedSamples: Int,
    /** How much of [fedSamples] was silence this harness added rather than audio the file held. */
    val paddedSilenceSamples: Int,
)

/**
 * Drives one online recognizer through one utterance as **chunked simulated streaming**.
 *
 * **This is the method ticket 40 exists to get right.** An online recognizer cannot be
 * scored on a whole-file decode: feeding it the entire waveform before the first `decode`
 * call does not produce a streaming measurement, it produces a different system that happens
 * to use the same classes, and the resulting WER would sit in a column labelled "streaming".
 * So the audio goes forward in `chunkMillis` pieces through the real `acceptWaveform` /
 * `isReady` / `decode` / `isEndpoint` loop, and the scored text is the **endpoint result** —
 * the one the recognizer itself declares final.
 *
 * The audio is fed as fast as the machine decodes it, not at wall-clock pace. That is what
 * makes RTF measurable at all, and it has one consequence worth stating: the trailing silence
 * an endpoint needs is *compressed away* rather than waited out. The padding this harness
 * appends is real audio to the recognizer — it produces frames and costs decode time — but it
 * costs no wall clock, so [OnlinePassResult.speechEndToFinalMs] measures the work and not the
 * waiting. [OnlinePassResult.paddedSilenceSamples] records how much was padded so a reader can
 * see which numbers that affects.
 *
 * @param session an [OnlineSession] the caller owns and will `reset()`; one pass is one
 *   utterance, and `reset` is how the next utterance starts on the same stream.
 */
fun measureOnlinePass(
    session: OnlineSession,
    samples: FloatArray,
    sampleRate: Int,
    chunkMillis: Int,
    trailingSilenceMillis: Int,
    clock: MonotonicClock = MonotonicClock.SYSTEM,
): OnlinePassResult {
    val start = clock.nanos()
    val chunkSamples = (chunkMillis * sampleRate) / 1000
    require(chunkSamples > 0) { "chunk_ms=$chunkMillis is under one sample at $sampleRate Hz" }

    var fed = 0
    var padded = 0
    var endpointReached = false
    var finalText: String? = null
    var lastFeedStartedAtNanos = start
    var firstPartialMs: Double? = null
    var lastPartialAtMs = 0.0
    var lastPartialText: String? = null
    val partialGaps = mutableListOf<Double>()

    /** Feed one piece, drain every ready decode, then read the result. */
    fun feed(piece: FloatArray) {
        lastFeedStartedAtNanos = clock.nanos()
        session.accept(piece, sampleRate)
        fed += piece.size
        while (session.isReady()) session.decode()
        val text = session.resultText()
        if (text.isNotEmpty() && text != lastPartialText) {
            val at = fed * 1000.0 / sampleRate
            if (firstPartialMs == null) firstPartialMs = at else partialGaps += at - lastPartialAtMs
            lastPartialAtMs = at
            lastPartialText = text
        }
    }

    /** The endpoint is a state the recognizer is *in*, so it is read after every drain. */
    fun endpointIfReached(): Boolean {
        if (!session.isEndpoint()) return false
        endpointReached = true
        finalText = session.resultText()
        return true
    }

    var offset = 0
    while (offset < samples.size) {
        val end = minOf(offset + chunkSamples, samples.size)
        feed(samples.copyOfRange(offset, end))
        offset = end
        if (endpointIfReached()) break
    }

    if (!endpointReached) {
        // Endpointing cannot fire inside a tail shorter than `minTrailingSilence`, which is
        // 2.4 s by default — see `EndpointRule` in the artifact. `model-selection.md`'s
        // protocol supplies that tail and BenchmarkManifestReader refuses a manifest that does
        // not, so this padding is the plan's requirement rather than an invention here.
        //
        // It is rounded UP to a whole number of chunks, which is also where the grace for a
        // frame boundary comes from: feeding exactly 2400 ms can leave the final millisecond
        // unsatisfied, and the overshoot is a fraction of one chunk. The loop is bounded by the
        // target, so a recognizer that never endpoints ends the pass and says so rather than
        // padding indefinitely in someone's hand.
        val target = ((trailingSilenceMillis * sampleRate) / 1000).coerceAtLeast(1)
        val silence = FloatArray(chunkSamples)
        while (padded < target) {
            feed(silence)
            padded += chunkSamples
            if (endpointIfReached()) break
        }
    }

    if (!endpointReached) {
        // `inputFinished()` is the online stream's flush: it tells the recognizer no more audio
        // is coming, which is what makes a *short* utterance endpoint instead of waiting for a
        // tail that will never arrive. Reached only when the padding was not enough, so the
        // common path does not pay for it.
        session.inputFinished()
        while (session.isReady()) session.decode()
        endpointIfReached()
    }

    val endNanos = clock.nanos()
    return OnlinePassResult(
        // No endpoint means no Final. The trailing partial is the recognizer's best guess so
        // far; scoring it in the WER column would credit the model with a completeness it did
        // not demonstrate, so it is dropped and [endpointReached] says why.
        finalText = if (endpointReached) finalText else null,
        endpointReached = endpointReached,
        firstPartialMs = firstPartialMs,
        partialIntervalMs = Summary.of(partialGaps)?.median,
        endpointToFinalMs = null,
        speechEndToFinalMs = if (endpointReached) (endNanos - lastFeedStartedAtNanos) / 1_000_000.0 else null,
        decodeMillis = (endNanos - start) / 1_000_000.0,
        fedSamples = fed,
        paddedSilenceSamples = padded,
    )
}
