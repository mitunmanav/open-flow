package dev.openflow.dictation.providers.sherpa.benchmark

import java.io.Reader
import java.util.Properties

/**
 * A run manifest that cannot be understood, or that asks for something the plan
 * does not allow.
 *
 * Every failure this harness can detect at parse time is thrown as this rather
 * than repaired. The reason is specific to what this instrument is for: it
 * produces the numbers the V1 model default is chosen from, and a benchmark that
 * recovers from a typo reports a clean run over a matrix that is not the one the
 * plan states. A loud stop is recoverable; a plausible wrong number is not.
 */
class BenchmarkConfigurationException(message: String) : IllegalArgumentException(message)

/**
 * Where in the plan's three-device spread this run was made.
 *
 * The tier is not decoration. `model-selection.md`'s RTF threshold is written
 * against **the weakest tier**, and the project has one or two phones, so most
 * runs will be on a device that is not it. [ThresholdVerdict] uses this to decide
 * whether a run may be reported as a gate result at all, and the answer is no
 * unless the weakest tier is among the tiers actually run.
 */
enum class DeviceTier(val manifestValue: String) {
    WEAKEST("weakest"),
    MID("mid"),
    HEADROOM("headroom");

    companion object {
        fun parse(raw: String): DeviceTier = entries.firstOrNull { it.manifestValue == raw.trim().lowercase() }
            ?: throw BenchmarkConfigurationException(
                "device.tier='$raw' is not one of ${entries.joinToString(", ") { it.manifestValue }}"
            )
    }
}

/**
 * Where an eval utterance's reference text comes from.
 *
 * The two layouts exist because LibriSpeech test-clean — the eval set the plan
 * names for English — does not ship one transcript per audio file. It ships a
 * single `test-clean.trans.txt` of `utterance-id text` lines covering every
 * utterance, which is a different shape from the per-file `.txt` sidecar a
 * hand-recorded zh/en dictation set uses. Supporting only one of them would mean
 * either the English set or the dictation set is unscoreable, and an unscoreable
 * WER column is worse than a missing one because it looks like a zero.
 */
enum class ReferenceLayout(val manifestValue: String) {
    /** `<audio>.txt` beside each audio file. */
    PER_FILE_SIDECAR("per-file"),

    /** One `id text` transcript file for the whole set — LibriSpeech's layout. */
    LIBRISPEECH_TRANSCRIPT("librispeech");

    companion object {
        fun parse(raw: String): ReferenceLayout = entries.firstOrNull { it.manifestValue == raw.trim().lowercase() }
            ?: throw BenchmarkConfigurationException(
                "eval_set.reference='$raw' is not one of ${entries.joinToString(", ") { it.manifestValue }}"
            )
    }
}

/** The device a run was made on, as the acceptance gate's Device Class discipline requires it be recorded. */
data class DeviceRecord(
    val name: String,
    val tier: DeviceTier,
    val abi: String,
    val oemSkin: String,
    val androidMajor: Int,
    val notes: String,
)

/** The fixed eval set. Its [unit] is a property of the set, never of the model — see [ScoreUnit]. */
data class EvalSet(
    val dir: String,
    val pattern: String,
    val unit: ScoreUnit,
    val reference: ReferenceLayout,
    /** Only for [ReferenceLayout.LIBRISPEECH_TRANSCRIPT]. */
    val transcriptPath: String?,
)

/**
 * Silero VAD settings, which the offline/simulated path needs and the online path
 * does not.
 *
 * These are the demo-conventional Silero values, stated rather than inherited:
 * `model-selection.md` fixes `windowSize` 512 and `sherpa-onnx.md` fixes sample
 * rate 16000, and the rest are configuration rather than model identity. They are
 * in the manifest so a run can record exactly what it used — a VAD threshold is
 * part of what an endpoint→Final number means, because the segmentation decides
 * where the endpoint is.
 */
data class VadSettings(
    val model: String,
    val windowSize: Int,
    val threshold: Float,
    val minSilenceSeconds: Float,
    val minSpeechSeconds: Float,
    val maxSpeechSeconds: Float,
)

/**
 * The measurement protocol, from `model-selection.md`'s *Method*.
 *
 * [warmupIterations] and [measuredIterations] are carried here rather than
 * hardcoded because the plan fixes their values but not their constancy, and a
 * harness that silently used different ones would still print the plan's numbers
 * in its report.
 */
data class Protocol(
    val threadCounts: List<Int>,
    val warmupIterations: Int,
    val measuredIterations: Int,
    val chunkMillis: Int,
    val trailingSilenceMillis: Int,
    val executionProvider: String,
)

/**
 * One model to measure, with the files it needs.
 *
 * [files] maps a **role** to a filename within [directory]. Roles are the
 * constructor arguments the resolved AAR actually takes — verified with `javap`
 * against `sherpa-onnx-v1.13.8.aar`, not read off the upstream docs, because
 * `sherpa-onnx.md` records that the docs were three releases stale and wrong
 * about four signatures. Roles rather than filenames in the API because the
 * filenames are the part that legitimately varies between upstream releases, and
 * a hardcoded ONNX name is a claim nobody can check until a phone fails to open
 * it.
 */
data class ModelEntry(
    val candidate: ModelCandidate,
    val directory: String,
    val files: Map<String, String>,
    val decodingMethod: String,
    val language: String?,
    val task: String?,
    val modelType: String?,
) {
    fun pathOf(role: String): String = "$directory/${files.getValue(role)}"
}

/** Everything one benchmark run needs, parsed and validated. */
data class BenchmarkManifest(
    val device: DeviceRecord,
    val evalSet: EvalSet,
    val vad: VadSettings,
    val protocol: Protocol,
    val models: List<ModelEntry>,
    val batterySessionMinutes: Int,
    val notes: String,
)

/**
 * Reader and validator for the run manifest.
 *
 * **Why a properties file and not JSON.** The manifest is typed by a person
 * holding a phone, once, before a run that takes tens of minutes. `Properties`
 * needs no dependency, is readable and commentable in any editor, and — unlike
 * `org.json` — is the *same implementation* in a JVM unit test and on a device, so
 * the parsing that CI verifies is the parsing that runs. A hand-rolled JSON
 * parser would have been the alternative and would have been a hundred lines of
 * bug surface in exchange for nesting this schema does not need.
 *
 * Nesting is expressed with dotted key prefixes (`model.3.encoder=...`) and
 * consecutive integer indices, which is how properties files express a list
 * without a schema.
 */
object BenchmarkManifestReader {

    /**
     * Roles each family's recognizer constructor requires.
     *
     * Every one of these is a constructor parameter read off the resolved AAR with
     * `javap`:
     * - `OnlineTransducerModelConfig(encoder, decoder, joiner, qnnConfig)`
     * - `OnlineParaformerModelConfig(encoder, decoder)`
     * - `OfflineWhisperModelConfig(encoder, decoder, language, task, …)`
     * - `OfflineMoonshineModelConfig(preprocessor, encoder, uncachedDecoder, cachedDecoder, mergedDecoder)`
     * - `OfflineSenseVoiceModelConfig(model, language, useInverseTextNormalization, qnnConfig)`
     * - `OfflineNemoEncDecCtcModelConfig(model)`
     *
     * `tokens` is required by all six because it lives on the enclosing
     * `OnlineModelConfig` / `OfflineModelConfig` rather than the family config,
     * and a recognizer built without it is built wrong rather than built loosely.
     *
     * Moonshine takes either a split `decoder` (uncached + cached, v1 releases)
     * or a `mergedDecoder` (v2 releases) depending on what the release ships —
     * which of the two is a property of the download, not of the family, so the
     * choice is the manifest's and the harness reports which shape it used.
     */
    val REQUIRED_ROLES: Map<RecognizerFamily, Set<String>> = mapOf(
        RecognizerFamily.ONLINE_TRANSDUCER to setOf("encoder", "decoder", "joiner", "tokens"),
        RecognizerFamily.ONLINE_PARAFORMER to setOf("encoder", "decoder", "tokens"),
        RecognizerFamily.WHISPER to setOf("encoder", "decoder", "tokens"),
        RecognizerFamily.MOONSHINE to setOf("preprocessor", "encoder", "tokens"),
        RecognizerFamily.SENSE_VOICE to setOf("model", "tokens"),
        RecognizerFamily.NEMO_CTC to setOf("model", "tokens"),
    )

    /** Roles that satisfy Moonshine's decoder, one of which a release will have. */
    val MOONSHINE_DECODER_ROLES = setOf("decoder", "merged_decoder")

    /** The plan's floor on measured iterations. */
    const val MIN_MEASURED_ITERATIONS = 20

    /** The plan's floor on discarded warmup decodes. */
    const val MIN_WARMUP_ITERATIONS = 5

    /**
     * `EndpointConfig.rule1`'s default `minTrailingSilence`, in milliseconds.
     *
     * Endpointing cannot fire inside a shorter tail than this, so a run that pads
     * less than it and then reports endpoint→Final is reporting the time to
     * *manufacture* an endpoint rather than the time to reach one. Validated
     * rather than trusted, because the failure is silent and the number is wrong
     * rather than absent.
     */
    const val MIN_TRAILING_SILENCE_MS = 2400

    fun parse(reader: Reader): BenchmarkManifest {
        val props = Properties().apply {
            // `load(Reader)`, not `load(InputStream)`. The stream overload assumes
            // ISO-8859-1, which would turn a note written in anything else into
            // mojibake; the Reader overload reads the characters the caller already
            // decoded, so the encoding is the caller's decision and is made at the
            // point the file is opened.
            load(reader)
        }
        return from(props)
    }

    internal fun from(props: Properties): BenchmarkManifest {
        fun required(key: String): String =
            props.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw BenchmarkConfigurationException(
                    "The run manifest is missing '$key'. Every key below is required; " +
                        "a run that guessed one would report a number for a protocol nobody agreed to."
                )

        fun optional(key: String, fallback: String): String =
            props.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

        fun int(key: String, fallback: Int? = null): Int {
            val raw = props.getProperty(key)?.trim()
            if (raw.isNullOrEmpty()) {
                return fallback ?: throw BenchmarkConfigurationException("The run manifest is missing '$key'.")
            }
            return raw.toIntOrNull() ?: throw BenchmarkConfigurationException("'$key'='$raw' is not a whole number.")
        }

        fun float(key: String, fallback: Float): Float {
            val raw = props.getProperty(key)?.trim()
            if (raw.isNullOrEmpty()) return fallback
            return raw.toFloatOrNull() ?: throw BenchmarkConfigurationException("'$key'='$raw' is not a number.")
        }

        val device = DeviceRecord(
            name = required("device.name"),
            tier = DeviceTier.parse(required("device.tier")),
            abi = required("device.abi"),
            oemSkin = required("device.oem_skin"),
            androidMajor = int("device.android_major").also {
                if (it < 1) throw BenchmarkConfigurationException("device.android_major='$it' is not an Android version.")
            },
            notes = optional("device.notes", ""),
        )

        val evalSet = EvalSet(
            dir = required("eval_set.dir"),
            pattern = optional("eval_set.pattern", "*.wav"),
            unit = ScoreUnit.entries.firstOrNull { it.name == required("eval_set.unit").uppercase() }
                ?: throw BenchmarkConfigurationException(
                    "eval_set.unit must be WORD or CHARACTER. It is a property of the eval set, not of the model: " +
                        "two of the candidates are bilingual, so a harness that inferred it would compare two " +
                        "different quantities in one table."
                ),
            reference = ReferenceLayout.parse(optional("eval_set.reference", ReferenceLayout.PER_FILE_SIDECAR.manifestValue)),
            transcriptPath = props.getProperty("eval_set.transcript")?.trim()?.takeIf { it.isNotEmpty() },
        )
        if (evalSet.reference == ReferenceLayout.LIBRISPEECH_TRANSCRIPT && evalSet.transcriptPath.isNullOrEmpty()) {
            throw BenchmarkConfigurationException(
                "eval_set.reference=librispeech needs eval_set.transcript naming the single " +
                    "'id text' transcript file. LibriSpeech test-clean ships one transcript for the whole set, " +
                    "not one per audio file."
            )
        }

        val vad = VadSettings(
            model = required("vad.model"),
            windowSize = int("vad.window_size", 512).also {
                if (it <= 0) throw BenchmarkConfigurationException("vad.window_size='$it' must be positive.")
            },
            threshold = float("vad.threshold", 0.5f),
            minSilenceSeconds = float("vad.min_silence_s", 0.5f),
            minSpeechSeconds = float("vad.min_speech_s", 0.25f),
            maxSpeechSeconds = float("vad.max_speech_s", 20f),
        )

        val threadCounts = required("threads").split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map {
                it.toIntOrNull()?.takeIf { n -> n >= 1 }
                    ?: throw BenchmarkConfigurationException("threads='$it' is not a positive whole number.")
            }
            .distinct()
        if (threadCounts.isEmpty()) throw BenchmarkConfigurationException("threads listed no values.")
        // Deliberately NOT fatal when a requested width exceeds the device's core
        // count. A phone reporting few cores is common, and refusing to run would
        // lose the measurement rather than qualify it. The width the device
        // actually gave is recorded in the report beside the width requested, so
        // `threads=2,4` on a 2-core device reads as what happened and not as what
        // was asked for — see RunReport's `requestedThreads` / `cpuCount` pair.

        val protocol = Protocol(
            threadCounts = threadCounts,
            warmupIterations = int("warmup_iterations", MIN_WARMUP_ITERATIONS).also {
                if (it < 0) throw BenchmarkConfigurationException("warmup_iterations='$it' is negative.")
            },
            measuredIterations = int("measured_iterations", MIN_MEASURED_ITERATIONS).also {
                if (it < MIN_MEASURED_ITERATIONS) {
                    throw BenchmarkConfigurationException(
                        "measured_iterations=$it is below the plan's floor of $MIN_MEASURED_ITERATIONS. " +
                            "model-selection.md's Method fixes the median of at least 20 iterations, and a median " +
                            "over fewer is not the number the plan's thresholds were written against."
                    )
                }
            },
            chunkMillis = int("chunk_ms", 100).also {
                if (it <= 0) throw BenchmarkConfigurationException("chunk_ms='$it' must be positive.")
            },
            trailingSilenceMillis = int("trailing_silence_ms", MIN_TRAILING_SILENCE_MS).also {
                if (it < 0) throw BenchmarkConfigurationException("trailing_silence_ms='$it' is negative.")
                if (it < MIN_TRAILING_SILENCE_MS) {
                    throw BenchmarkConfigurationException(
                        "trailing_silence_ms=$it is below the $MIN_TRAILING_SILENCE_MS ms endpoint rule's " +
                            "minTrailingSilence. Endpointing cannot fire inside a shorter tail, so the run would " +
                            "measure how long it takes to manufacture an endpoint rather than to reach one."
                    )
                }
            },
            executionProvider = optional("provider", "cpu").also {
                if (it != "cpu") {
                    throw BenchmarkConfigurationException(
                        "provider='$it'. model-selection.md's Method fixes provider=\"cpu\" for the matrix; " +
                            "QNN and RKNN are excluded from V1 and benchmarking them here would put a number in " +
                            "the results document that no shipped device uses."
                    )
                }
            },
        )

        val models = parseModels(props)

        return BenchmarkManifest(
            device = device,
            evalSet = evalSet,
            vad = vad,
            protocol = protocol,
            models = models,
            batterySessionMinutes = int("battery_session_minutes", 10).also {
                if (it < 1) throw BenchmarkConfigurationException("battery_session_minutes='$it' must be at least 1.")
            },
            notes = optional("notes", ""),
        )
    }

    private fun parseModels(props: Properties): List<ModelEntry> {
        // `model.<n>.<key>`, n counted from 1. Indices need not be contiguous —
        // a person deleting the third of four entries should not silently shift
        // the fourth one's meaning — but they must start at 1 and be unique, or
        // "which model is 3" stops having an answer.
        val indices = props.stringPropertyNames()
            .mapNotNull { key ->
                Regex("^model\\.(\\d+)\\.").find(key)?.groupValues?.get(1)?.toIntOrNull()
            }
            .distinct()
            .sorted()

        if (indices.isEmpty()) {
            throw BenchmarkConfigurationException(
                "The run manifest lists no models. Add a model.<n>.id line per candidate; " +
                    "ModelMatrixTemplate writes a commented template naming all of them."
            )
        }
        if (indices.first() != 1) {
            throw BenchmarkConfigurationException("model indices must start at 1; found ${indices.first()}.")
        }

        return indices.map { n ->
            fun modelKey(key: String) = props.getProperty("model.$n.$key")?.trim()
            fun requiredRole(role: String) =
                modelKey(role)?.takeIf { it.isNotEmpty() }
                    ?: throw BenchmarkConfigurationException("model.$n (${modelKey("id")}) is missing the '$role' file.")

            val candidate = ModelMatrix.require(modelKey("id") ?: throw BenchmarkConfigurationException("model.$n has no id."))

            val required = REQUIRED_ROLES.getValue(candidate.family)
            val optionalRoles = if (candidate.family == RecognizerFamily.MOONSHINE) MOONSHINE_DECODER_ROLES else emptySet()
            val files = buildMap {
                for (role in required) put(role, requiredRole(role))
                for (role in optionalRoles) modelKey(role)?.takeIf { it.isNotEmpty() }?.let { put(role, it) }
            }

            // A Moonshine entry with neither decoder shape is the one case where
            // the required-role check above passes and the recognizer would still
            // be built wrong, because its constructor takes the split pair.
            if (candidate.family == RecognizerFamily.MOONSHINE && !files.keys.any { it in MOONSHINE_DECODER_ROLES }) {
                throw BenchmarkConfigurationException(
                    "model.$n (${candidate.id}) needs a 'decoder' (v1 releases ship uncached + cached) or a " +
                        "'merged_decoder' (v2 releases ship one file). Which of the two is a property of the release."
                )
            }

            ModelEntry(
                candidate = candidate,
                directory = modelKey("dir")
                    ?: throw BenchmarkConfigurationException("model.$n (${candidate.id}) has no dir."),
                files = files,
                // Conventionally "greedy_search" for every family in this matrix:
                // the plan runs no language model, and beam search over no LM is
                // the same result more slowly. Supplied by the manifest rather
                // than hardcoded because it is a value, not a fact about the API,
                // and a model that transcribes nothing is usually this.
                decodingMethod = modelKey("decoding_method") ?: "greedy_search",
                language = modelKey("language"),
                task = modelKey("task"),
                modelType = modelKey("model_type"),
            )
        }
    }

    /**
     * A commented, ready-to-edit manifest naming every candidate in
     * [ModelMatrix.CANDIDATES].
     *
     * Generated from the matrix rather than maintained as a file beside it, so
     * the template cannot name a candidate the harness does not know. The
     * `RUNTIME NOTE` block is the part a person actually needs: the paths are
     * device paths, and sherpa's release layouts differ per model, which is why
     * the file names are left blank rather than guessed.
     */
    fun template(): String = buildString {
        appendLine("# OpenFlow — sherpa-onnx API-level benchmark run manifest")
        appendLine("#")
        appendLine("# Generated from ModelMatrix by BenchmarkManifestReader.template(), so it cannot name a")
        appendLine("# candidate the harness does not know about. Paths are paths ON THE DEVICE.")
        appendLine("# Procedure: docs/providers/model-benchmark-harness.md")
        appendLine()
        appendLine("# ---- the device this run was made on -------------------------------------------")
        appendLine("# tier drives whether the thresholds may be reported as a gate result; see the")
        appendLine("# 'Not yet specified' note in model-selection.md and ThresholdVerdict.")
        appendLine("device.name = ")
        appendLine("device.tier = mid                 # weakest | mid | headroom")
        appendLine("device.abi = arm64-v8a")
        appendLine("device.oem_skin = ")
        appendLine("device.android_major = ")
        appendLine("device.notes = ")
        appendLine()
        appendLine("# ---- the fixed eval set -------------------------------------------------------")
        appendLine("# unit is the SET's property. English sets are WORD; a zh dictation set is CHARACTER,")
        appendLine("# because Chinese has no word spaces and a word-level score there is 0% or 100%.")
        appendLine("eval_set.dir = ")
        appendLine("eval_set.unit = WORD               # WORD | CHARACTER")
        appendLine("eval_set.pattern = *.wav")
        appendLine("eval_set.reference = per-file      # per-file | librispeech")
        appendLine("# Only for reference=librispeech, which ships ONE 'id text' file for the whole set:")
        appendLine("eval_set.transcript = ")
        appendLine()
        appendLine("# ---- the VAD, which the offline/simulated path needs ---------------------------")
        appendLine("vad.model = ")
        appendLine("vad.window_size = 512")
        appendLine("vad.threshold = 0.5")
        appendLine("vad.min_silence_s = 0.5")
        appendLine("vad.min_speech_s = 0.25")
        appendLine("vad.max_speech_s = 20.0")
        appendLine()
        appendLine("# ---- protocol, from model-selection.md's Method ------------------------------")
        appendLine("threads = 2, 4")
        appendLine("warmup_iterations = $MIN_WARMUP_ITERATIONS")
        appendLine("measured_iterations = $MIN_MEASURED_ITERATIONS")
        appendLine("chunk_ms = 100")
        appendLine("trailing_silence_ms = $MIN_TRAILING_SILENCE_MS")
        appendLine("provider = cpu")
        appendLine("battery_session_minutes = 10")
        appendLine("notes = ")
        appendLine()
        appendLine("# ---- the candidates ----------------------------------------------------------")
        appendLine("#")
        appendLine("# RUNTIME NOTE — read before filling this in:")
        appendLine("#   * dir is the model directory ON THE DEVICE, not a download URL. Fetch each")
        appendLine("#     release from the sherpa-onnx asr-models release, extract it, and push it:")
        appendLine("#       adb push <extracted>/ /sdcard/openflow-models/")
        appendLine("#   * the file names are deliberately BLANK. They differ per model and per upstream")
        appendLine("#     release, and this project has already shipped four wrong ones written from")
        appendLine("#     prose (ticket 32). List the directory and copy the names from what you")
        appendLine("#     actually downloaded; the harness reports any it cannot find.")
        appendLine("#   * int8 only. fp32 is a spot check on one device if RTF fails, not a matrix entry.")
        appendLine()
        ModelMatrix.CANDIDATES.forEachIndexed { index, candidate ->
            val n = index + 1
            appendLine()
            appendLine("# ${candidate.displayName} — ${candidate.family} (${candidate.family.scoringStrategy})")
            if (candidate.planToken.isEmpty()) {
                appendLine("# NOT a plan matrix row: this is the quality reference for another candidate, and")
                appendLine("# is here because \"+2 absolute versus the larger model\" is unverifiable unless the")
                appendLine("# larger model is runnable too. Optional; skip it to save download and time.")
            }
            appendLine("model.$n.id = ${candidate.id}")
            appendLine("model.$n.dir = ")
            for (role in REQUIRED_ROLES.getValue(candidate.family).sorted()) {
                appendLine("model.$n.$role = ")
            }
            if (candidate.family == RecognizerFamily.MOONSHINE) {
                appendLine("# Exactly one of these two, matching what the release ships:")
                appendLine("model.$n.decoder = ")
                appendLine("model.$n.merged_decoder = ")
            }
            if (candidate.family == RecognizerFamily.WHISPER) {
                appendLine("model.$n.language = en")
                appendLine("model.$n.task = transcribe")
            }
            if (candidate.family == RecognizerFamily.SENSE_VOICE) {
                appendLine("model.$n.language = auto")
            }
            if (candidate.family == RecognizerFamily.NEMO_CTC) {
                appendLine("model.$n.model_type = enc_dec_ctc")
            }
            appendLine("model.$n.decoding_method = greedy_search")
        }
    }
}
