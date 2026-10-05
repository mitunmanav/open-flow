package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * Which sherpa-onnx recognizer a candidate is driven through.
 *
 * This is the single most consequential field in the matrix, because it decides
 * **which measurement code runs**. Ticket 40's warning is the reason it is a
 * closed enum rather than a string in the manifest: an online model scored on a
 * whole-file decode, or an offline model fed through the endpointing loop, both
 * produce a confident number and both measure a system nobody ships.
 */
enum class RecognizerFamily {
    /** Online transducer — the zipformer streaming family. Partials and endpointing. */
    ONLINE_TRANSDUCER,

    /** Online paraformer. Partials and endpointing, no joiner. */
    ONLINE_PARAFORMER,

    /** Offline Whisper. One result per utterance, VAD-segmented. */
    WHISPER,

    /** Offline Moonshine. One result per utterance, VAD-segmented. */
    MOONSHINE,

    /** Offline SenseVoice. One result per utterance, VAD-segmented. */
    SENSE_VOICE,

    /** Offline NeMo encoder-decoder CTC — the parakeet-TDT family. */
    NEMO_CTC;

    /** Online families stream and endpoint; offline families are VAD-segmented. */
    val isStreaming: Boolean
        get() = this == ONLINE_TRANSDUCER || this == ONLINE_PARAFORMER

    /**
     * How the WER column is produced for this family.
     *
     * Online models are scored on **chunked simulated streaming** — the audio is
     * fed forward in `chunk_ms` pieces through the real accept/decode loop, and
     * the score comes from the endpoint result. A whole-file decode is not an
     * available shortcut here: an online recognizer fed the entire waveform
     * before any `decode` call is not producing a streaming measurement, it is
     * producing a different number that looks like one.
     *
     * Offline models are scored on **VAD-segmented simulated streaming** for the
     * same reason. `sherpa-onnx.md` records this as the `SherpaOnnxSimulateStreamingAsr`
     * shape, and it is the only way an offline model yields a per-utterance result
     * that a dictation user would ever have waited for.
     */
    val scoringStrategy: ScoringStrategy
        get() = if (isStreaming) ScoringStrategy.CHUNKED_STREAMING else ScoringStrategy.VAD_SEGMENTED
}

/** How a family's audio is fed, and therefore how it is scored. See [RecognizerFamily.scoringStrategy]. */
enum class ScoringStrategy { CHUNKED_STREAMING, VAD_SEGMENTED }

/**
 * One row of `model-selection.md`'s benchmark matrix.
 *
 * **What this type owns, and what it deliberately does not.**
 *
 * It owns the facts that change *what gets measured*: which upstream release the
 * candidate is, which recognizer drives it, how its audio is fed, and what unit
 * its score is in. Those are structural, they change the code path, and if the
 * plan and the harness disagree about one of them the benchmark is measuring
 * something other than what the plan claims.
 *
 * It does **not** own the model file names. Those are supplied by the run
 * manifest, because they are packaging rather than identity, and because this
 * project has already been bitten by writing an unverified filename into a
 * document: ticket 32 found four such claims in `sherpa-onnx.md`, one of which
 * described code that would not compile, and upstream's own docs page was three
 * releases stale while it did it. A hardcoded ONNX filename here would be the
 * same mistake in a place where the compiler cannot check it — the file names
 * would simply fail to open at run time, on a phone, in front of a person
 * holding the only copy of the data. So the manifest names the files and the
 * harness reports which of them it could not find.
 *
 * [planToken] is the substring of `model-selection.md`'s `**Matrix:**` line that
 * names this candidate. It exists so `ModelMatrixTest` can assert the two lists
 * are the same list — a ratchet, because a ninth candidate added to the plan and
 * not to this file would otherwise be silently absent from every benchmark run
 * this project ever does, with nothing reporting the gap.
 */
data class ModelCandidate(
    val id: String,
    val displayName: String,
    val family: RecognizerFamily,
    /** Exact substring naming this candidate in the plan's matrix line. */
    val planToken: String,
    /**
     * The candidate this one is allowed to lose to by up to `+2 absolute` and
     * still be considered acceptable — the larger model in the same family, per
     * `model-selection.md`'s WER-regression rule. `null` when the candidate has
     * no larger sibling in the matrix, in which case there is no regression to
     * measure and the rule reports nothing rather than comparing it to itself.
     */
    val qualityReferenceId: String? = null,
)

/**
 * The eight candidates in `model-selection.md`'s benchmark matrix, in the plan's
 * order.
 *
 * The order is the plan's, not a ranking: the plan's *Decision rule* picks the
 * smallest model in each family that passes thresholds, so a reader comparing
 * this list against the plan should be able to do it by eye.
 */
object ModelMatrix {

    val CANDIDATES: List<ModelCandidate> = listOf(
        ModelCandidate(
            id = "streaming-zipformer-en-20M-2023-02-17",
            displayName = "streaming zipformer en 20M",
            family = RecognizerFamily.ONLINE_TRANSDUCER,
            planToken = "en-20M zipformer",
            // The V1 default per ADR-0010, downloaded rather than bundled. Its
            // quality reference is the larger English streaming zipformer, which
            // is the comparison the "+2 absolute" rule exists for.
            qualityReferenceId = "streaming-zipformer-en-2023-06-26",
        ),
        ModelCandidate(
            id = "streaming-zipformer-en-2023-06-26",
            displayName = "streaming zipformer en 2023-06-26",
            family = RecognizerFamily.ONLINE_TRANSDUCER,
            planToken = "en-2023-06-26 zipformer",
        ),
        ModelCandidate(
            id = "streaming-zipformer-small-bilingual-zh-en-2023-02-16",
            displayName = "streaming zipformer small bilingual zh-en",
            family = RecognizerFamily.ONLINE_TRANSDUCER,
            planToken = "small-bilingual-zh-en",
            qualityReferenceId = "streaming-zipformer-bilingual-zh-en-2023-02-20",
        ),
        ModelCandidate(
            id = "streaming-zipformer-bilingual-zh-en-2023-02-20",
            // Not a Matrix row of its own: the plan's matrix line names eight
            // candidates and this is the quality reference for the row above. It
            // is here because a reference that cannot be measured is not a
            // reference — "+2 absolute versus the larger model" is unverifiable
            // unless the larger model is runnable.
            displayName = "streaming zipformer bilingual zh-en",
            family = RecognizerFamily.ONLINE_TRANSDUCER,
            planToken = "",
        ),
        ModelCandidate(
            id = "streaming-paraformer-bilingual-zh-en",
            displayName = "streaming paraformer bilingual zh-en",
            family = RecognizerFamily.ONLINE_PARAFORMER,
            planToken = "streaming paraformer bilingual",
        ),
        ModelCandidate(
            id = "whisper-tiny.en",
            displayName = "whisper tiny.en (int8)",
            family = RecognizerFamily.WHISPER,
            planToken = "whisper-tiny.en",
        ),
        ModelCandidate(
            id = "moonshine-tiny-en-int8",
            displayName = "moonshine tiny en (int8)",
            family = RecognizerFamily.MOONSHINE,
            planToken = "moonshine-tiny-en",
        ),
        ModelCandidate(
            id = "sensevoice-zh-en-ja-ko-yue-2024-07-17",
            displayName = "SenseVoice zh-en-ja-ko-yue",
            family = RecognizerFamily.SENSE_VOICE,
            planToken = "sensevoice",
        ),
        ModelCandidate(
            id = "parakeet-tdt-0.6b-v2",
            displayName = "parakeet TDT 0.6b v2 (en)",
            family = RecognizerFamily.NEMO_CTC,
            planToken = "parakeet-tdt-v2",
        ),
    )

    /** The eight the plan's matrix line names, i.e. excluding quality references. */
    val PLANNED: List<ModelCandidate> = CANDIDATES.filter { it.planToken.isNotEmpty() }

    private val byId = CANDIDATES.associateBy { it.id }

    fun find(id: String): ModelCandidate? = byId[id]

    /**
     * The candidate, or a thrown error naming what *is* available.
     *
     * A manifest naming an unknown model is a typo, and a benchmark that quietly
     * skipped it would report a clean run over seven of eight candidates. The
     * failure names the ids that do exist so the fix does not require reading this
     * file.
     */
    fun require(id: String): ModelCandidate = byId[id] ?: throw BenchmarkConfigurationException(
        "'$id' is not a candidate in model-selection.md's benchmark matrix. " +
            "Known candidates: ${byId.keys.joinToString(", ")}"
    )
}
