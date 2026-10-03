package dev.openflow.dictation.providers.sherpa.modelstore

/**
 * One downloadable model: where its archive lives, which bytes it must
 * be, and which entries inside it OpenFlow needs.
 *
 * Every field is pinned, and the pinning is the point. The URL names the
 * one canonical source — the per-model `.tar.bz2` on sherpa-onnx's
 * `asr-models` GitHub release, the only distribution
 * `docs/providers/sherpa-onnx.md` documents — and the hash is the
 * expected SHA-256 of that exact archive, committed to this repository
 * after a human verified it against the release. Together they make the
 * ModelStore's integrity check meaningful: a checksum computed from the
 * bytes just downloaded would prove only that storage did not corrupt
 * them, while a hash pinned here proves the bytes have not changed
 * since a human read them. A re-uploaded or tampered asset fails the
 * check and the download is discarded.
 *
 * @param name The model's directory name: the archive's single
 *   top-level entry, and the name the extracted model occupies under
 *   the ModelStore's root. Every required entry must live inside it.
 * @param archiveUrl The pinned download URL.
 * @param archiveSha256 The expected SHA-256 of the full archive — 64
 *   lowercase hex characters over every byte of the `.tar.bz2`,
 *   including entries OpenFlow never extracts. The archive as a whole
 *   is the unit of integrity even though extraction is selective.
 * @param requiredEntries The archive-relative paths the provider loads,
 *   e.g. `"<name>/encoder-epoch-99-avg-1.int8.onnx"`. Extraction
 *   writes exactly these entries and nothing else, which is both the
 *   disk-space decision (the real archives also carry fp32 variants
 *   and test waves the app never loads) and the path-traversal guard:
 *   a name not in this set can never be opened for writing.
 */
data class ModelSpec(
    val name: String,
    val archiveUrl: String,
    val archiveSha256: String,
    val requiredEntries: Set<String>,
) {
    init {
        require(archiveSha256.matches(SHA_256_PATTERN)) {
            "archiveSha256 must be 64 lowercase hex characters, was '$archiveSha256'"
        }
        require(requiredEntries.isNotEmpty()) { "requiredEntries must not be empty" }
        requiredEntries.forEach { entry ->
            require(entry.startsWith("$name/")) {
                "entry '$entry' must live inside the model's top-level directory '$name/'"
            }
            require(!entry.contains("..") && !entry.startsWith('/')) {
                "entry '$entry' must be a plain path inside '$name/'"
            }
        }
    }

    /** [entry]'s path relative to the extracted model directory. */
    fun relativePath(entry: String): String = entry.substringAfter('/')

    /** [entry]'s file name, e.g. `"encoder-epoch-99-avg-1.int8.onnx"`. */
    fun baseName(entry: String): String = entry.substringAfterLast('/')

    private companion object {
        val SHA_256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

/**
 * The models OpenFlow V1 downloads.
 *
 * The V1 default streaming model is the first candidate of
 * `docs/providers/model-selection.md`'s matrix, and ADR-0010's
 * delivery decision: the Silero VAD is bundled in app assets (it is
 * not here — this object holds only what is downloaded), and this
 * model is fetched on first launch. The pinned hash was computed from
 * the `asr-models` release asset on 2026-10-03 (ticket 46);
 * re-verify against the release and re-pin before changing any field,
 * because the hash is the only thing standing between a user and a
 * changed upstream artifact.
 */
object V1Models {
    val STREAMING_EN_20M = ModelSpec(
        name = "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17",
        archiveUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/" +
            "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2",
        archiveSha256 = "9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9",
        requiredEntries = setOf(
            "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/encoder-epoch-99-avg-1.int8.onnx",
            "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/decoder-epoch-99-avg-1.int8.onnx",
            "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/joiner-epoch-99-avg-1.int8.onnx",
            "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/tokens.txt",
        ),
    )
}
