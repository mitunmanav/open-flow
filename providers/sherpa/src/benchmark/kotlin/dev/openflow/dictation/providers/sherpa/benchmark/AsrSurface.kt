package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * The five sherpa-onnx calls the two measurement passes make, behind interfaces.
 *
 * **Why an interface at all, when the real classes are right there.** Because the
 * measurement logic is the part that can be wrong in a way no compiler catches. Chunked
 * simulated streaming — the thing ticket 40 warns is easy to get wrong and that silently
 * turns the WER column into a different measurement — is arithmetic over when a partial
 * appears, when the endpoint fires, and what text is in hand at each point. Written against
 * a real `OnlineRecognizer` it can only be exercised by a person holding a phone; written
 * against these four interfaces, a JVM unit test drives every branch with a scripted fake
 * and no model file at all.
 *
 * The sherpa-backed implementations live in `androidTest`, which is where they belong: the
 * instrument must not reach the release AAR, and it must not reach the unit-test classpath
 * either, or loading a class would try to load `libsherpa-onnx-jni.so`.
 *
 * **The signatures here are the artifact's, not the docs'.** `accept` takes a sample rate
 * on both stream interfaces and `acceptToVad` does not, because
 * `OnlineStream.acceptWaveform(float[], int)` and `OfflineStream.acceptWaveform(float[], int)`
 * take two arguments while `Vad.acceptWaveform(float[])` takes one — verified with `javap`
 * against `sherpa-onnx-v1.13.8.aar`. Keeping the asymmetry visible in the type is the point:
 * a copy-paste between the two is a compile error rather than a wrong measurement.
 */
interface OnlineAsr {
    /** A fresh stream. sherpa reuses one stream across utterances with `reset()`; [OnlineSession.reset] exposes that. */
    fun createSession(): OnlineSession

    fun release()
}

interface OnlineSession {
    /** `OnlineStream.acceptWaveform(samples, sampleRate)` — **two** arguments. */
    fun accept(samples: FloatArray, sampleRate: Int)

    /** `OnlineStream.inputFinished()`. Tells the recognizer no more audio is coming. */
    fun inputFinished()

    /** `OnlineRecognizer.isReady(stream)`. */
    fun isReady(): Boolean

    /** `OnlineRecognizer.decode(stream)`. One call; call it only while [isReady]. */
    fun decode()

    /** `OnlineRecognizer.isEndpoint(stream)`. */
    fun isEndpoint(): Boolean

    /** `OnlineRecognizer.getResult(stream).text`. */
    fun resultText(): String

    /** `OnlineRecognizer.reset(stream)`. */
    fun reset()

    /** `OnlineStream.release()`. */
    fun release()
}

/** One offline decode: feed a whole segment, get one result. */
interface OfflineAsr {
    /** `OfflineStream.acceptWaveform(samples, sampleRate)` then `decode` then `getResult().text`. */
    fun decode(samples: FloatArray, sampleRate: Int): String

    fun release()
}

/** One speech segment the VAD closed, in absolute sample positions from the start of the file. */
class VadSegment(val startSample: Int, val samples: FloatArray) {
    /** Where the segment ends, exclusive. `SpeechSegment` carries only `start` and `samples`. */
    val endSample: Int get() = startSample + samples.size
}

/**
 * The VAD, as the offline path uses it.
 *
 * [consumedSampleCount] is not part of sherpa-onnx's `Vad`; it is fed by the harness as it
 * counts the samples it has accepted, and it is what makes the VAD's own latency measurable.
 * See [VadSegmentedPass] for why the segmentation cost has to be accounted for explicitly
 * rather than hoped to appear in a decode timer.
 */
interface VadSegmenter {
    /** `Vad.acceptWaveform(samples)` — **one** argument. This is the trap the artifact records. */
    fun acceptToVad(samples: FloatArray)

    /** `Vad.front()` / `Vad.pop()` until `Vad.empty()`, in order. */
    fun drain(): List<VadSegment>

    /** `Vad.flush()` — forces the tail segment out at end of stream. */
    fun flush()

    /** How many samples the VAD has been given so far, including any still held internally. */
    val consumedSampleCount: Int

    fun release()
}

/**
 * A monotonic clock, so a measurement pass is deterministic under test.
 *
 * `System.nanoTime()` is the only real implementation. The interface exists because every
 * number in [CellResult] except RTF's denominator and the WER counts is a wall-clock
 * duration, and a test that cannot control the clock can only assert "positive".
 */
interface MonotonicClock {
    fun nanos(): Long

    companion object {
        val SYSTEM: MonotonicClock = object : MonotonicClock {
            override fun nanos(): Long = System.nanoTime()
        }
    }
}

internal fun MonotonicClock.millisSince(startNanos: Long): Double = (nanos() - startNanos) / 1_000_000.0
