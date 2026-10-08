package dev.openflow.dictation.providers.sherpa.benchmark

import java.io.File
import java.io.IOException

/** One utterance of the eval set: the audio, and the text it should produce. */
data class Utterance(
    /** The audio file's name without its extension. LibriSpeech's transcript key is this. */
    val id: String,
    val audio: File,
    val reference: String,
)

/**
 * The fixed eval set, resolved on the device, in one of the two layouts `model-selection.md`
 * implies.
 *
 * **Every failure here is thrown, and none of them is a skip.** The alternative — dropping
 * an utterance whose reference is missing and scoring the rest — produces a WER column that
 * is arithmetically valid and completely misleading, because the denominator silently
 * shrank to the utterances that happened to be well-formed. That is the specific way this
 * harness could report a clean run over a corpus that is not the one the plan names, so
 * [EvalSetCatalog.utterances] either returns the whole set or throws.
 */
class EvalSetCatalog(
    val directory: File,
    val pattern: String,
    val referenceLayout: ReferenceLayout,
    val transcript: File?,
    val unit: ScoreUnit,
) {

    /**
     * Every utterance, in a stable order.
     *
     * Sorted by filename rather than by whatever `listFiles` returned, so two runs over the
     * same directory score the same corpus in the same sequence regardless of filesystem
     * order — and so a partial run over the first N is a prefix rather than an arbitrary
     * subset.
     */
    fun utterances(): List<Utterance> {
        if (!directory.isDirectory) {
            throw IOException("eval_set.dir='${directory.path}' is not a directory on this device.")
        }
        val audio = directory.listFiles { f -> f.isFile && f.name.matches(globToRegex(pattern)) }
            ?.sortedBy { it.name }
            .orEmpty()
        if (audio.isEmpty()) {
            throw IOException(
                "eval_set.dir='${directory.path}' holds no file matching eval_set.pattern='$pattern'. " +
                    "Nothing to score: a run over zero utterances would report a WER of null, which looks " +
                    "like a failure rather than like the absence of a corpus."
            )
        }
        return when (referenceLayout) {
            ReferenceLayout.PER_FILE_SIDECAR -> audio.map { file ->
                val sidecar = File(file.parentFile, "${file.nameWithoutExtension}.txt")
                if (!sidecar.isFile) {
                    throw IOException(
                        "${file.name} has no reference beside it (expected '${sidecar.name}'). " +
                            "eval_set.reference is 'per-file', so every audio file needs a same-named .txt."
                    )
                }
                Utterance(file.nameWithoutExtension, file, sidecar.readText())
            }

            ReferenceLayout.LIBRISPEECH_TRANSCRIPT -> {
                val path = transcript
                    ?: throw IOException("eval_set.reference=librispeech needs eval_set.transcript.")
                if (!path.isFile) throw IOException("eval_set.transcript='${path.path}' is not a file on this device.")
                val table = parseLibrispeechTranscript(path)
                audio.map { file ->
                    val text = table[file.nameWithoutExtension]
                        ?: throw IOException(
                            "${file.name} has no line in '${path.name}'. LibriSpeech ships one 'id text' file " +
                                "for the whole set; an audio file with no line is an incomplete copy, not a " +
                                "utterance to be skipped."
                        )
                    Utterance(file.nameWithoutExtension, file, text)
                }
            }
        }
    }

    /**
     * Reads a LibriSpeech `test-clean.trans.txt`: `utterance-id text` per line.
     *
     * Lines are `trim`med and blank ones dropped, but nothing else is rewritten — the WER
     * normalizer in [WordErrorRate] is the one place case and punctuation are settled, and
     * doing it twice would make the corpus text disagree with what the scorer actually saw.
     */
    private fun parseLibrispeechTranscript(path: File): Map<String, String> =
        path.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .associate { line ->
                val split = line.indexOf(' ')
                if (split <= 0) {
                    throw IOException("'${path.name}' has a line with no 'id text' split: '$line'.")
                }
                // The text is trimmed too, not just the line: a hand-edited transcript with two
                // spaces after the id would otherwise carry that leading whitespace into the
                // reference, where the WER normalizer would collapse it — a difference nobody can
                // see and everybody would have to reason about.
                line.substring(0, split) to line.substring(split + 1).trim()
            }

    /**
     * A shell glob to a regex, supporting `*` and `?` and nothing else.
     *
     * Hand-rolled rather than pulling in a glob library for one `*.wav`: the pattern comes
     * from a manifest a person typed, and an unsupported construct should quietly behave
     * like a literal rather than throw on a phone in someone's hand.
     */
    private fun globToRegex(pattern: String): Regex {
        val out = StringBuilder()
        for (ch in pattern) {
            when (ch) {
                '*' -> out.append(".*")
                '?' -> out.append('.')
                '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> out.append('\\').append(ch)
                else -> out.append(ch)
            }
        }
        return Regex(out.toString(), RegexOption.IGNORE_CASE)
    }
}
