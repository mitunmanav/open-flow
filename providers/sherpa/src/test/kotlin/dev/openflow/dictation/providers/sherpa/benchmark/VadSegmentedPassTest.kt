package dev.openflow.dictation.providers.sherpa.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The VAD-segmented simulated streaming path, driven by scripted VAD and offline recognizer.
 *
 * The quantity under test is the one the plan singles out: **endpoint→Final for offline models
 * including VAD segmentation cost**. The trap is that the harness feeds audio as fast as it
 * decodes, so the VAD's min-silence wait costs no wall clock at all, and a stopwatch around the
 * decode would report a number missing its largest live-session term. It is accounted for in
 * audio time instead, from what the VAD actually consumed rather than from what it was
 * configured to do.
 *
 * The asymmetry the artifact records is enforced structurally: the VAD's accept takes **one**
 * argument and both streams' take two. `acceptToVad(samples)` cannot be called the way
 * `accept(samples, sampleRate)` is, so a copy-paste between the two is a compile error rather
 * than a wrong measurement on a phone.
 */
class VadSegmentedPassTest {

    private class SettableClock(var now: Long = 0L) : MonotonicClock {
        override fun nanos(): Long = now
        fun advance(ms: Double) {
            now += (ms * 1_000_000).toLong()
        }
    }

    /**
     * A VAD that closes segments at scripted points in the audio.
     *
     * A rule fires when the sample count it names has been consumed, which is what makes the
     * wait term observable: the harness sees the segment at a known consumed count and derives
     * how much audio the VAD looked at past the segment's end before surfacing it.
     */
    private class ScriptedVad(
        private val rules: List<Rule> = emptyList(),
        private val flushSegmentSamples: Int? = null,
        private val msPerAccept: Double = 0.0,
        private val clock: SettableClock? = null,
    ) : VadSegmenter {
        data class Rule(val closeWhenConsumedAtLeast: Int, val startSample: Int, val samples: Int)

        val acceptSizes = mutableListOf<Int>()
        var flushes = 0
        var releases = 0
        var consumed = 0
            private set
        private val pending = mutableListOf<VadSegment>()
        private var nextRule = 0

        override fun acceptToVad(samples: FloatArray) {
            acceptSizes += samples.size
            consumed += samples.size
            clock?.advance(msPerAccept)
            while (nextRule < rules.size && consumed >= rules[nextRule].closeWhenConsumedAtLeast) {
                val rule = rules[nextRule++]
                pending += VadSegment(rule.startSample, FloatArray(rule.samples))
            }
        }

        override fun drain(): List<VadSegment> {
            val out = pending.toList()
            pending.clear()
            return out
        }

        override fun flush() {
            flushes++
            flushSegmentSamples?.let { pending += VadSegment(consumed - it, FloatArray(it)) }
        }

        override val consumedSampleCount: Int get() = consumed

        override fun release() {
            releases++
        }
    }

    private class FakeOfflineAsr(
        private val msPerDecode: Double = 0.0,
        private val clock: SettableClock? = null,
    ) : OfflineAsr {
        val decodedSizes = mutableListOf<Int>()
        var releases = 0
        override fun decode(samples: FloatArray, sampleRate: Int): String {
            // The offline stream's `acceptWaveform` takes two arguments, like the online one and
            // unlike the VAD's. A harness that dropped the rate here would not compile.
            assertEquals("the offline stream's accept carries the sample rate", 16_000, sampleRate)
            decodedSizes += samples.size
            clock?.advance(msPerDecode)
            return "text of ${samples.size} samples"
        }
        override fun release() {
            releases++
        }
    }

    private val sampleRate = 16_000
    private val chunkSamples = 1_600

    private fun speech(seconds: Double): FloatArray = FloatArray((seconds * sampleRate).toInt()) { 0.5f }
    private fun silence(seconds: Double): FloatArray = FloatArray((seconds * sampleRate).toInt())

    // ---- feeding --------------------------------------------------------------------

    @Test
    fun audioIsHandedToTheVadOneArgumentAtATimeInChunks() {
        val vad = ScriptedVad()
        measureOfflinePass(vad, FakeOfflineAsr(), speech(1.0), sampleRate, chunkMillis = 100)
        assertEquals(List(10) { chunkSamples }, vad.acceptSizes)
        assertEquals(16_000, vad.consumed)
        assertEquals(16_000, vad.consumedSampleCount)
    }

    // ---- the segments ---------------------------------------------------------------

    @Test
    fun oneFinalPerSegmentAndNoWholeFileDecode() {
        // The `SherpaOnnxSimulateStreamingAsr` shape: a bounded utterance, one Final per
        // utterance. The recognizer is never handed the whole file, because a single 30-second
        // decode is a system nobody ever waits for.
        val vad = ScriptedVad(
            rules = listOf(
                ScriptedVad.Rule(closeWhenConsumedAtLeast = 40_000, startSample = 0, samples = 32_000),
                ScriptedVad.Rule(closeWhenConsumedAtLeast = 64_000, startSample = 40_000, samples = 16_000),
            )
        )
        val asr = FakeOfflineAsr()
        val audio = speech(2.0) + silence(1.0) + speech(1.0)
        val result = measureOfflinePass(vad, asr, audio, sampleRate, chunkMillis = 100)
        assertEquals(2, result.segmentCount)
        assertEquals(listOf(32_000, 16_000), asr.decodedSizes)
        assertTrue("no decode saw the whole file", asr.decodedSizes.none { it == audio.size })
        assertEquals(48_000, result.scoredSamples)
    }

    @Test
    fun theTranscriptsAreReportedPerSegmentInOrder() {
        val vad = ScriptedVad(
            rules = listOf(
                ScriptedVad.Rule(closeWhenConsumedAtLeast = 40_000, startSample = 0, samples = 32_000),
                ScriptedVad.Rule(closeWhenConsumedAtLeast = 64_000, startSample = 40_000, samples = 16_000),
            )
        )
        val result = measureOfflinePass(vad, FakeOfflineAsr(), speech(2.0) + silence(1.0) + speech(1.0), sampleRate, 100)
        assertEquals(
            listOf("text of 32000 samples", "text of 16000 samples"),
            result.segmentTexts,
        )
    }

    @Test
    fun flushForcesOutTheTailSegmentADictationWouldOtherwiseLose() {
        // The plan's eval set is read speech whose trailing silence is shorter than the VAD's
        // min-silence, so without `flush()` the last utterance of every file would score
        // nothing — and a corpus that silently loses its last utterance is the failure this
        // harness is built to make loud.
        val vad = ScriptedVad(flushSegmentSamples = 32_000)
        val result = measureOfflinePass(vad, FakeOfflineAsr(), speech(2.0), sampleRate, chunkMillis = 100)
        assertEquals(1, vad.flushes)
        assertEquals(1, result.segmentCount)
        assertEquals("text of 32000 samples", result.segmentTexts.single())
        assertEquals(32_000, result.scoredSamples)
    }

    @Test
    fun aFileWithNoSpeechHasNoSegmentsAndNoFinalToBeLateFor() {
        // Zero segments is a legitimate outcome, and the quantity compared against the 1 s bar
        // does not exist. It is null, not zero — a zero would clear the bar.
        val vad = ScriptedVad(flushSegmentSamples = null)
        val result = measureOfflinePass(vad, FakeOfflineAsr(), silence(2.0), sampleRate, chunkMillis = 100)
        assertEquals(0, result.segmentCount)
        assertNull(result.endpointToFinalMs)
        assertNull(result.speechEndToFinalMs)
        assertEquals(0, result.scoredSamples)
    }

    // ---- the segmentation cost ------------------------------------------------------

    @Test
    fun theVadWaitIsAccountedForEvenThoughItCostsNoWallClock() {
        // 0.25 ms of VAD compute per 100 ms chunk and 400 ms of decode per segment, on a clock
        // that never advances during the half second of silence the VAD looked at. The reported
        // figure is 500 + 7.5 + 400 = 907.5 ms. A stopwatch around the decode alone would have
        // said "400 ms, comfortably inside the 1 s bar" — the exact misreading this accounting
        // exists to prevent.
        val clock = SettableClock()
        val vad = ScriptedVad(
            rules = listOf(ScriptedVad.Rule(closeWhenConsumedAtLeast = 40_000, startSample = 0, samples = 32_000)),
            msPerAccept = 0.25,
            clock = clock,
        )
        val audio = speech(2.0) + silence(1.0)
        val result = measureOfflinePass(vad, FakeOfflineAsr(msPerDecode = 400.0, clock = clock), audio, sampleRate, 100, clock)
        assertEquals(1, result.segmentCount)
        // Segment ends at 32 000 samples and surfaced at 40 000 consumed: 8 000 samples = 0.5 s.
        assertEquals(500.0, result.vadWaitMillis, 1e-6)
        assertEquals(400.0, result.decodeMillis, 1e-6)
        // 30 chunks of 0.25 ms, every one of them inside the measured window.
        assertEquals(7.5, result.vadComputeMillis, 1e-6)
        assertEquals(907.5, result.endpointToFinalMs!!, 1e-6)
        assertEquals(
            "for the VAD-segmented path the two origins are the same event",
            result.endpointToFinalMs,
            result.speechEndToFinalMs,
        )
    }

    @Test
    fun theVadWaitIsWhatTheVadActuallyConsumedAndNotWhatItsSettingsSay() {
        // Two segments, surfacing 16 000 and 8 000 samples past their ends. Neither number is
        // derivable from a configured `minSilenceDuration` — they are what this VAD did, which
        // is why the manifest records its settings: they are part of what the figure means.
        val vad = ScriptedVad(
            rules = listOf(
                ScriptedVad.Rule(closeWhenConsumedAtLeast = 48_000, startSample = 0, samples = 32_000),
                ScriptedVad.Rule(closeWhenConsumedAtLeast = 64_000, startSample = 48_000, samples = 8_000),
            )
        )
        val result = measureOfflinePass(vad, FakeOfflineAsr(), speech(4.0), sampleRate, 100)
        assertEquals(2, result.segmentCount)
        assertEquals(1_500.0, result.vadWaitMillis, 1e-6)
    }

    @Test
    fun aSegmentClosedByFlushHasNoWait() {
        // `flush()` pops the tail with nothing left to consume, so the wait term is zero.
        val vad = ScriptedVad(flushSegmentSamples = 32_000)
        val result = measureOfflinePass(vad, FakeOfflineAsr(), speech(2.0), sampleRate, 100)
        assertEquals(0.0, result.vadWaitMillis, 1e-9)
    }

    @Test
    fun theWaitIsNeverNegativeWhenAVadClosesBeforeItsOwnBuffer() {
        // `coerceAtLeast(0)`: a VAD that surfaces a segment with audio unconsumed would otherwise
        // subtract from the answer and make a slow system look faster.
        val vad = ScriptedVad(
            rules = listOf(ScriptedVad.Rule(closeWhenConsumedAtLeast = 1_600, startSample = 0, samples = 32_000))
        )
        val result = measureOfflinePass(vad, FakeOfflineAsr(), speech(2.0), sampleRate, 100)
        assertEquals(0.0, result.vadWaitMillis, 1e-9)
    }

    @Test
    fun endpointToFinalIsTheSumOfTheThreeTermsAndNotTheDecodeAlone() {
        val clock = SettableClock()
        val vad = ScriptedVad(
            rules = listOf(ScriptedVad.Rule(closeWhenConsumedAtLeast = 40_000, startSample = 0, samples = 32_000)),
            msPerAccept = 0.25,
            clock = clock,
        )
        val audio = speech(2.0) + silence(1.0)
        val result = measureOfflinePass(vad, FakeOfflineAsr(msPerDecode = 700.0, clock = clock), audio, sampleRate, 100, clock)
        assertEquals(
            result.vadWaitMillis + result.vadComputeMillis + result.decodeMillis,
            result.endpointToFinalMs!!,
            1e-9,
        )
        assertTrue(
            "and the decode alone would have hidden the segmentation cost",
            result.decodeMillis < result.endpointToFinalMs!!,
        )
    }

    @Test
    fun thePassWallClockIsTheRtfNumeratorAndIncludesTheVad() {
        val clock = SettableClock()
        val vad = ScriptedVad(
            rules = listOf(ScriptedVad.Rule(closeWhenConsumedAtLeast = 16_000, startSample = 0, samples = 16_000)),
            msPerAccept = 0.25,
            clock = clock,
        )
        val asr = FakeOfflineAsr(msPerDecode = 5.0, clock = clock)
        val result = measureOfflinePass(vad, asr, speech(1.0), sampleRate, 100, clock)
        assertEquals(clock.now / 1_000_000.0, result.passMillis, 1e-6)
        assertTrue("the VAD's own compute is inside the measured pass", result.passMillis > result.decodeMillis)
    }

    @Test
    fun aChunkSmallerThanOneSampleIsRefusedRatherThanDividingByZero() {
        try {
            measureOfflinePass(ScriptedVad(), FakeOfflineAsr(), speech(1.0), sampleRate, chunkMillis = 0)
            throw AssertionError("expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("chunk_ms"))
        }
    }
}
