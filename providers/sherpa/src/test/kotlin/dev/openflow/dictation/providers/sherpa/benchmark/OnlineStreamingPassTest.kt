package dev.openflow.dictation.providers.sherpa.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chunked simulated streaming path, driven by a scripted recognizer.
 *
 * **This is where the WER column is earned.** `model-selection.md` fixes `chunk_ms`, and the
 * plan warns that an online model cannot be scored on a whole-file decode. Everything the WER
 * depends on — that the audio arrives in pieces, that the scored text is the *endpoint* result
 * and not the last partial, that a missing endpoint yields no score at all — is asserted here
 * against a fake that behaves like sherpa, so it is checked on every CI run rather than on the
 * one afternoon a phone is available.
 *
 * The fake mirrors the API's actual shape, including the three facts that are easy to get wrong
 * from prose: `accept` carries a sample rate, `isReady` goes false once the frames are consumed
 * (so a fake that always says true hangs the pass, as it should), and the endpoint result is a
 * different string from the last partial.
 */
class OnlineStreamingPassTest {

    private class SettableClock(var now: Long = 0L) : MonotonicClock {
        override fun nanos(): Long = now
        fun advance(ms: Double) {
            now += (ms * 1_000_000).toLong()
        }
    }

    private class FakeOnlineSession(
        private val partialEverySamples: Int = 1_600,
        private val endpointAfterSamples: Int = 38_400,
        private val endpointOnInputFinished: Boolean = true,
        private val clock: SettableClock? = null,
        private val msPerDecode: Double = 0.0,
    ) : OnlineSession {
        /** sherpa's online feature frame at 16 kHz is 10 ms, so 160 samples. */
        private val frameSamples = 160

        var decodes = 0
        var inputFinishedCalls = 0
        var resets = 0
        var releases = 0
        val acceptSizes = mutableListOf<Int>()
        var lastSampleRate: Int? = null
        private var undecoded = 0
        private var fedSamples = 0
        private var silentSamples = 0
        private var nextPartialAt = partialEverySamples
        private var text = ""
        private var endpointed = false

        override fun accept(samples: FloatArray, sampleRate: Int) {
            acceptSizes += samples.size
            lastSampleRate = sampleRate
            undecoded += samples.size
            var loud = 0
            for (sample in samples) if (sample != 0f) loud++
            if (loud == samples.size) {
                fedSamples += samples.size
                silentSamples = 0
                while (fedSamples >= nextPartialAt) {
                    text = "word${nextPartialAt / partialEverySamples}"
                    nextPartialAt += partialEverySamples
                }
            } else {
                silentSamples += samples.size
                if (silentSamples >= endpointAfterSamples) endpointed = true
            }
        }

        override fun inputFinished() {
            inputFinishedCalls++
            if (endpointOnInputFinished) endpointed = true
        }

        override fun isReady(): Boolean = undecoded >= frameSamples

        override fun decode() {
            decodes++
            undecoded = (undecoded - frameSamples).coerceAtLeast(0)
            clock?.advance(msPerDecode)
        }

        override fun isEndpoint(): Boolean = endpointed

        override fun resultText(): String = text

        override fun reset() {
            resets++
        }

        override fun release() {
            releases++
        }
    }

    private val sampleRate = 16_000
    private val chunkSamples = 1_600

    private fun speech(seconds: Double): FloatArray =
        FloatArray((seconds * sampleRate).toInt()) { 0.5f }

    private fun measure(
        session: OnlineSession,
        seconds: Double = 0.5,
        chunkMillis: Int = 100,
        trailingSilenceMillis: Int = 2400,
        clock: MonotonicClock = MonotonicClock.SYSTEM,
    ) = measureOnlinePass(session, speech(seconds), sampleRate, chunkMillis, trailingSilenceMillis, clock)

    // ---- the scored text is the endpoint result -------------------------------------

    @Test
    fun theScoredTextIsTheEndpointResultNotTheLastPartial() {
        // The partial says "word2" and the endpoint upgrades it to "word2 word3 final". Scoring
        // the partial instead would report the recognizer mid-sentence as if it had finished,
        // which is the whole-file-decode mistake in a different costume.
        val delegate = FakeOnlineSession(endpointAfterSamples = 8_000)
        val session = object : OnlineSession by delegate {
            override fun resultText(): String =
                if (delegate.isEndpoint()) "word2 word3 final" else delegate.resultText()
        }
        val result = measure(session)
        assertTrue(result.endpointReached)
        assertEquals("word2 word3 final", result.finalText)
    }

    @Test
    fun audioArrivesInChunksOfTheManifestChunkSize() {
        val session = FakeOnlineSession()
        measure(session, seconds = 1.0, trailingSilenceMillis = 100)
        // 1.0 s at 100 ms per chunk is ten chunks of 1600 samples. Every accept carries the
        // sample rate: `OnlineStream.acceptWaveform(float[], int)` takes two arguments, and the
        // artifact is the only authority on that.
        assertEquals(List(10) { chunkSamples }, session.acceptSizes.take(10))
        assertEquals(sampleRate, session.lastSampleRate!!)
    }

    @Test
    fun everyFrameIsDecodedAndTheDrainStopsWhenNoneAreReady() {
        val session = FakeOnlineSession()
        measure(session, seconds = 1.0, trailingSilenceMillis = 100)
        // Eleven chunks of 160 samples' worth of 10 ms frames each. An implementation that
        // decoded without checking `isReady` would either loop forever or over-decode.
        assertEquals(11 * 10, session.decodes)
    }

    @Test
    fun theTrailingSilenceIsPaddedOnBecauseEndpointingCannotFireInsideAShorterTail() {
        // `EndpointRule.rule1.minTrailingSilence` is 2.4 s — verified with `javap -c` against
        // `sherpa-onnx-v1.13.8.aar`, not read off the docs — so a 0.5 s utterance fed on its own
        // never endpoints and never yields a Final.
        val session = FakeOnlineSession(endpointAfterSamples = 38_400)
        val result = measure(session)
        assertTrue("the endpoint never fired inside the file's own audio", result.paddedSilenceSamples > 0)
        assertEquals(2_400 * sampleRate / 1000, result.paddedSilenceSamples)
        assertTrue(result.endpointReached)
    }

    @Test
    fun paddingIsRoundedUpToWholeChunksSoAFrameBoundaryCannotLeaveItShort() {
        // 2450 ms is not a whole number of 100 ms chunks. Feeding exactly 2450 ms can leave the
        // last chunk short of the rule's 2.4 s, so the tail is padded up to the next whole
        // chunk. The overshoot is the grace; it is a fraction of one chunk and it is recorded.
        val session = FakeOnlineSession(endpointAfterSamples = 39_100)
        val result = measure(session, trailingSilenceMillis = 2450)
        assertTrue(result.endpointReached)
        val target = 2450 * sampleRate / 1000
        assertTrue(
            "the padded tail must reach the requested 2450 ms; was ${result.paddedSilenceSamples} samples",
            result.paddedSilenceSamples >= target,
        )
        assertEquals("padding is whole chunks", 0, result.paddedSilenceSamples % chunkSamples)
        assertTrue(
            "and it must not overshoot by a whole extra chunk",
            result.paddedSilenceSamples < target + chunkSamples,
        )
    }

    @Test
    fun noEndpointMeansNoFinalAndNoScoreRatherThanTheTrailingPartial() {
        // A recognizer that never endpoints — a model built without endpointing, a threshold
        // nothing satisfies — must produce no transcript to score. Scoring the trailing partial
        // would credit the model with a completeness it did not demonstrate, and the WER column
        // would carry a number for an utterance the system never finished.
        val session = FakeOnlineSession(endpointAfterSamples = Int.MAX_VALUE, endpointOnInputFinished = false)
        val result = measure(session, trailingSilenceMillis = 500)
        assertFalse(result.endpointReached)
        assertNull(result.finalText)
        assertTrue("partials were produced, so the pass was not vacuous", session.decodes > 0)
    }

    @Test
    fun inputFinishedIsTheFlushForAnEndpointThatNeverFires() {
        // `inputFinished()` is the online stream's flush: it is what makes a short utterance
        // endpoint instead of waiting for a tail that will never arrive.
        val session = FakeOnlineSession(endpointAfterSamples = Int.MAX_VALUE)
        val result = measure(session, trailingSilenceMillis = 100)
        assertEquals(1, session.inputFinishedCalls)
        assertTrue(result.endpointReached)
    }

    @Test
    fun inputFinishedIsNotCalledOnTheCommonPath() {
        // Once the endpoint has fired the stream is finished; `inputFinished()` afterwards tells
        // a recognizer that has already declared its utterance complete that more audio is
        // coming, and pays decode time for the privilege.
        val session = FakeOnlineSession(endpointAfterSamples = 8_000)
        measure(session)
        assertEquals(0, session.inputFinishedCalls)
    }

    // ---- partials -------------------------------------------------------------------

    @Test
    fun firstPartialAndCadenceAreMeasuredInAudioTimeNotWallClock() {
        // A partial every 3200 samples — 200 ms of audio — on a recognizer that takes 5 ms of
        // wall clock per 10 ms frame, i.e. runs at half real time. Cadence is a property of the
        // model: measured in wall clock it would be audio time divided by RTF, so a slow device
        // would report a *better* first partial purely by taking longer to get there.
        val clock = SettableClock()
        val session = FakeOnlineSession(
            partialEverySamples = 3_200,
            endpointAfterSamples = Int.MAX_VALUE,
            clock = clock,
            msPerDecode = 5.0,
        )
        val result = measureOnlinePass(
            session, speech(2.0), sampleRate, chunkMillis = 100, trailingSilenceMillis = 100, clock = clock
        )
        assertEquals(200.0, result.firstPartialMs!!, 1e-6)
        assertEquals(200.0, result.partialIntervalMs!!, 1e-6)
        assertTrue(
            "the wall clock ran well past the audio, so the two units really do differ: " +
                "decodeMillis=${result.decodeMillis} firstPartialMs=${result.firstPartialMs}",
            result.decodeMillis > result.firstPartialMs!! * 5,
        )
    }

    @Test
    fun aRecognizerThatEmitsNoPartialReportsNoFirstPartial() {
        val session = object : OnlineSession by FakeOnlineSession() {
            override fun resultText(): String = ""
        }
        val result = measure(session, seconds = 1.0, trailingSilenceMillis = 100)
        assertNull(result.firstPartialMs)
        assertNull(result.partialIntervalMs)
    }

    @Test
    fun anUnchangedResultIsNotAPartialUpdate() {
        // sherpa's `getResult` returns the same text until it changes, so counting every call
        // would report a cadence equal to the chunk size for a model that emits one result.
        val session = object : OnlineSession by FakeOnlineSession() {
            override fun resultText(): String = "constant"
        }
        val result = measure(session, seconds = 1.0, trailingSilenceMillis = 100)
        assertEquals(100.0, result.firstPartialMs!!, 1e-6)
        assertNull("one distinct text is one partial, so there is no interval", result.partialIntervalMs)
    }

    // ---- the columns ----------------------------------------------------------------

    @Test
    fun endpointToFinalIsAbsentForAnOnlineModelBecauseItIsSatisfiedByConstruction() {
        // `isEndpoint()` true already means `getResult()` holds the finished text. The window is
        // a fraction of a millisecond for every online model forever, so it is reported absent
        // rather than as a passing number nobody measured.
        val result = measure(FakeOnlineSession(endpointAfterSamples = 8_000))
        assertTrue(result.endpointReached)
        assertNull(result.endpointToFinalMs)
        assertTrue(
            "the online figure a reader wants is the next column, and it is measured",
            result.speechEndToFinalMs != null,
        )
    }

    @Test
    fun thePassRecordsTheWallClockItSpentAndTheSamplesItFed() {
        val clock = SettableClock()
        val session = FakeOnlineSession(clock = clock, msPerDecode = 2.0)
        val result = measure(session, seconds = 1.0, trailingSilenceMillis = 100, clock = clock)
        assertEquals(session.decodes * 2.0, result.decodeMillis, 1e-6)
        assertTrue(
            "fed samples cover the file plus any padded tail",
            result.fedSamples >= speech(1.0).size,
        )
        assertEquals(16_000, result.paddedSilenceSamples)
    }

    @Test
    fun aChunkSmallerThanOneSampleIsRefusedRatherThanDividingByZero() {
        try {
            measure(FakeOnlineSession(), chunkMillis = 0)
            throw AssertionError("expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("chunk_ms"))
        }
    }
}
