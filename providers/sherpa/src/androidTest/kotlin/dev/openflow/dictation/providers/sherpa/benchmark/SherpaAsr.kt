package dev.openflow.dictation.providers.sherpa.benchmark

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineParaformerModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineParaformerModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.io.File

/**
 * sherpa-onnx's Kotlin API, wrapped in the four interfaces the measurement passes use.
 *
 * **This is the only file in the harness that names a sherpa class.** It lives in `androidTest`
 * because the instrument must not reach the release AAR, and because loading any of these types
 * loads `libsherpa-onnx-jni.so`, which a JVM unit test cannot do. Everything the measurement
 * logic needs from sherpa is behind [OnlineAsr], [OfflineAsr] and [VadSegmenter], so the logic
 * itself is exercised on every CI run by `OnlineStreamingPassTest` and `VadSegmentedPassTest`
 * with fakes, and this file is the thin part that could not be.
 *
 * **Every signature below was read off the resolved artifact with `javap`, not off the docs.**
 * `docs/providers/sherpa-onnx.md` records that upstream's own Android docs page was three
 * releases stale while it got four signatures wrong, one of which described code that would not
 * compile. The traps this file is written around:
 *
 * - `Vad.acceptWaveform(float[])` takes **one** argument. `OnlineStream.acceptWaveform` and
 *   `OfflineStream.acceptWaveform` both take **two** — `(float[], int sampleRate)`. Copying the
 *   sample rate into the VAD call, or dropping it from a stream call, is a compile error, which
 *   is the only reason this asymmetry is survivable.
 * - `OfflineModelConfig`'s sixth parameter is named **`nemo`**, not `nemoEncDecCtc`.
 * - `VadModelConfig`'s Silero knobs are `minSilenceDuration` / `minSpeechDuration` /
 *   `maxSpeechDuration`, and they live on [SileroVadModelConfig], not on `VadModelConfig`.
 *   Its units are **seconds**.
 * - `Vad.front()` returns a `SpeechSegment` with only `start` and `samples` — no end, so the
 *   harness computes the end itself.
 * - Asset-vs-file loading is the nullable `AssetManager` constructor argument. Models here are
 *   staged on the device filesystem by `adb push`, so it is `null`.
 */
internal object SherpaAsr {

    /** `model-selection.md`'s eval-set sample rate, and what every recognizer is built with. */
    const val SAMPLE_RATE = 16_000

    /** sherpa's standard log-mel dimension. */
    const val FEATURE_DIM = 80

    /**
     * sherpa's built-in endpoint rules, restated rather than read off the top-level
     * `getEndpointConfig()` helper.
     *
     * The values are verified rather than remembered: `javap -c` on
     * `com.k2fsa.sherpa.onnx.OnlineRecognizerKt.getEndpointConfig()` in
     * `sherpa-onnx-v1.13.8.aar` shows the bytecode constructing
     * `EndpointRule(false, 2.4f, 0.0f)`, `EndpointRule(true, 1.4f, 0.0f)` and
     * `EndpointRule(false, 0.0f, 20.0f)`. The 2.4 s `minTrailingSilence` is the figure
     * `BenchmarkManifestReader.MIN_TRAILING_SILENCE_MS` validates a manifest's tail against, and
     * `ThresholdVerdict` names in its endpoint note.
     *
     * Restated rather than called because a top-level `val` in a JNI-backed API is exactly the
     * kind of thing whose name and visibility move between releases, and a benchmark cannot have
     * its endpoint rule change under it. **Units are seconds**, not milliseconds — see the
     * conversion at the call site.
     */
    fun endpointConfig(): EndpointConfig = EndpointConfig(
        rule1 = EndpointRule(mustContainNonSilence = false, minTrailingSilence = 2.4f, minUtteranceLength = 0.0f),
        rule2 = EndpointRule(mustContainNonSilence = true, minTrailingSilence = 1.4f, minUtteranceLength = 0.0f),
        rule3 = EndpointRule(mustContainNonSilence = false, minTrailingSilence = 0.0f, minUtteranceLength = 20.0f),
    )

    /** The `minTrailingSilence` of [endpointConfig]'s rule1, in milliseconds. */
    val ENDPOINT_MIN_TRAILING_SILENCE_MS: Int get() = 2_400

    /**
     * The model files a manifest entry names that are **not** on the device.
     *
     * A run reports these per cell rather than throwing: a person staging eight models by hand
     * will get one wrong, and losing the whole matrix to it would be worse than a report that
     * says which cell was skipped and why.
     */
    fun missingFiles(entry: ModelEntry): List<String> = entry.files.map { (role, name) ->
        File(entry.pathOf(role)).path
    }.filterNot { File(it).isFile }

    // ---- online ---------------------------------------------------------------------

    fun online(entry: ModelEntry, numThreads: Int, assetManager: AssetManager?): OnlineAsr {
        val recognizer = OnlineRecognizer(
            assetManager = assetManager,
            config = OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = FEATURE_DIM),
                modelConfig = onlineModelConfig(entry, numThreads),
                endpointConfig = endpointConfig(),
                enableEndpoint = true,
                decodingMethod = entry.decodingMethod,
            ),
        )
        return SherpaOnlineAsr(recognizer)
    }

    private fun onlineModelConfig(entry: ModelEntry, numThreads: Int): OnlineModelConfig {
        val common = mapOf(
            "tokens" to entry.pathOf("tokens"),
            "numThreads" to numThreads,
            // `model-selection.md` fixes provider="cpu" for the matrix; QNN and RKNN are
            // excluded from V1, and BenchmarkManifestReader refuses a manifest that asks for one.
            "provider" to "cpu",
        )
        return when (entry.candidate.family) {
            RecognizerFamily.ONLINE_TRANSDUCER -> OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = entry.pathOf("encoder"),
                    decoder = entry.pathOf("decoder"),
                    joiner = entry.pathOf("joiner"),
                ),
                tokens = common["tokens"] as String,
                numThreads = numThreads,
                provider = common["provider"] as String,
                modelType = entry.modelType ?: "",
            )

            RecognizerFamily.ONLINE_PARAFORMER -> OnlineModelConfig(
                paraformer = OnlineParaformerModelConfig(
                    encoder = entry.pathOf("encoder"),
                    decoder = entry.pathOf("decoder"),
                ),
                tokens = common["tokens"] as String,
                numThreads = numThreads,
                provider = common["provider"] as String,
                modelType = entry.modelType ?: "",
            )

            else -> throw IllegalArgumentException(
                "${entry.candidate.displayName} is ${entry.candidate.family}, which is not an online family. " +
                    "Driving it through OnlineRecognizer would measure a system nobody ships."
            )
        }
    }

    // ---- offline --------------------------------------------------------------------

    fun offline(entry: ModelEntry, numThreads: Int, assetManager: AssetManager?): OfflineAsr {
        val recognizer = OfflineRecognizer(
            assetManager = assetManager,
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = FEATURE_DIM),
                modelConfig = offlineModelConfig(entry, numThreads),
                decodingMethod = entry.decodingMethod,
            ),
        )
        return SherpaOfflineAsr(recognizer)
    }

    private fun offlineModelConfig(entry: ModelEntry, numThreads: Int): OfflineModelConfig {
        val tokens = entry.pathOf("tokens")
        return when (entry.candidate.family) {
            RecognizerFamily.WHISPER -> OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = entry.pathOf("encoder"),
                    decoder = entry.pathOf("decoder"),
                    language = entry.language ?: "en",
                    task = entry.task ?: "transcribe",
                ),
                tokens = tokens,
                numThreads = numThreads,
                provider = "cpu",
                modelType = entry.modelType ?: "",
            )

            RecognizerFamily.MOONSHINE -> OfflineModelConfig(
                moonshine = OfflineMoonshineModelConfig(
                    preprocessor = entry.pathOf("preprocessor"),
                    encoder = entry.pathOf("encoder"),
                    // Five paths, of which a release uses either the split pair or the single
                    // merged file. BenchmarkManifestReader has already refused an entry that
                    // names neither or only half a pair.
                    uncachedDecoder = entry.files["uncached_decoder"] ?: "",
                    cachedDecoder = entry.files["cached_decoder"] ?: "",
                    mergedDecoder = entry.files["merged_decoder"] ?: "",
                ),
                tokens = tokens,
                numThreads = numThreads,
                provider = "cpu",
                modelType = entry.modelType ?: "",
            )

            RecognizerFamily.SENSE_VOICE -> OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = entry.pathOf("model"),
                    language = entry.language ?: "auto",
                    // The VAD's ITN output format is a version-specific spelling of numbers
                    // that LibriSpeech references never use. Off, so SenseVoice is scored on the
                    // same surface form as every other candidate; the tag says so in the report.
                    useInverseTextNormalization = false,
                ),
                tokens = tokens,
                numThreads = numThreads,
                provider = "cpu",
                modelType = entry.modelType ?: "",
            )

            RecognizerFamily.NEMO_CTC -> OfflineModelConfig(
                // The parameter is named `nemo`, not `nemoEncDecCtc`, and its type is
                // `OfflineNemoEncDecCtcModelConfig`. Verified with `javap`; the getter on
                // `OfflineModelConfig` is `getNemo()`.
                nemo = OfflineNemoEncDecCtcModelConfig(model = entry.pathOf("model")),
                tokens = tokens,
                numThreads = numThreads,
                provider = "cpu",
                modelType = entry.modelType ?: "enc_dec_ctc",
            )

            else -> throw IllegalArgumentException(
                "${entry.candidate.displayName} is ${entry.candidate.family}, which is not an offline family. " +
                    "Driving it through OfflineRecognizer would measure a system nobody ships."
            )
        }
    }

    // ---- VAD ------------------------------------------------------------------------

    fun vad(settings: VadSettings, numThreads: Int, assetManager: AssetManager?): VadSegmenter {
        // The Silero knobs live on `SileroVadModelConfig` and are in **seconds**;
        // `VadModelConfig` itself carries `sampleRate`, `numThreads`, `provider` and `debug`.
        // Passing 2400 where 2.4 is meant would be a 40-minute endpoint rule, so the manifest's
        // millisecond values are converted here and nowhere else.
        val config = VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(
                model = settings.model,
                threshold = settings.threshold,
                minSilenceDuration = settings.minSilenceSeconds,
                minSpeechDuration = settings.minSpeechSeconds,
                windowSize = settings.windowSize,
                maxSpeechDuration = settings.maxSpeechSeconds,
            ),
            sampleRate = SAMPLE_RATE,
            numThreads = numThreads,
            provider = "cpu",
        )
        return SherpaVadSegmenter(Vad(assetManager = assetManager, config = config))
    }
}

// ---- the wrappers -----------------------------------------------------------------

private class SherpaOnlineAsr(private val recognizer: OnlineRecognizer) : OnlineAsr {

    override fun createSession(): OnlineSession {
        val stream = recognizer.createStream()
        return object : OnlineSession {
            override fun accept(samples: FloatArray, sampleRate: Int) {
                // Two arguments. `Vad.acceptWaveform` takes one; this does not.
                stream.acceptWaveform(samples, sampleRate)
            }

            override fun inputFinished() = stream.inputFinished()
            override fun isReady(): Boolean = recognizer.isReady(stream)
            override fun decode() = recognizer.decode(stream)
            override fun isEndpoint(): Boolean = recognizer.isEndpoint(stream)
            override fun resultText(): String = recognizer.getResult(stream).text
            override fun reset() = recognizer.reset(stream)
            override fun release() = stream.release()
        }
    }

    override fun release() = recognizer.release()
}

private class SherpaOfflineAsr(private val recognizer: OfflineRecognizer) : OfflineAsr {

    override fun decode(samples: FloatArray, sampleRate: Int): String {
        // A fresh stream per segment: an `OfflineStream` accumulates, so reusing one across
        // segments would concatenate two utterances into one decode.
        val stream = recognizer.createStream()
        return try {
            stream.acceptWaveform(samples, sampleRate)
            recognizer.decode(stream)
            recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    override fun release() = recognizer.release()
}

/**
 * The VAD, counting what it was given.
 *
 * [VadSegmenter.consumedSampleCount] is not something sherpa-onnx reports — the harness counts
 * it — and it is what makes the VAD's own latency measurable. See [measureOfflinePass]: the
 * min-silence wait costs no wall clock in a run that feeds audio as fast as it decodes, so it is
 * accounted for in audio time from this count instead.
 */
private class SherpaVadSegmenter(private val vad: Vad) : VadSegmenter {

    private var consumed = 0

    override fun acceptToVad(samples: FloatArray) {
        // One argument. No sample rate.
        vad.acceptWaveform(samples)
        consumed += samples.size
    }

    override fun drain(): List<VadSegment> {
        val out = mutableListOf<VadSegment>()
        while (!vad.empty()) {
            val segment = vad.front()
            // Copied out: the samples belong to native memory that `pop()` releases, and the
            // decode happens after it.
            out += VadSegment(segment.start, segment.samples.copyOf())
            vad.pop()
        }
        return out
    }

    override fun flush() = vad.flush()

    override val consumedSampleCount: Int get() = consumed

    override fun release() = vad.release()
}
