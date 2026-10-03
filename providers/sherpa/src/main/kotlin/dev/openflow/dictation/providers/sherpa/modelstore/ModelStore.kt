package dev.openflow.dictation.providers.sherpa.modelstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest

/** What [ModelStore.ensureModel] did, or why it could not. */
sealed interface ModelStoreResult {
    /** The model was already extracted; nothing was downloaded. */
    data object AlreadyPresent : ModelStoreResult

    /** The archive was downloaded, verified against the pinned hash, and extracted. */
    data object Downloaded : ModelStoreResult

    /**
     * The model is not present and the store could not fetch it. The
     * reason is human-readable; the caller's response is the same either
     * way — surface it on ticket 47's first-launch screen and report
     * `MODEL_MISSING`, whose fix is a download prompt rather than a
     * retry (ADR-0001).
     */
    data class Failed(val reason: String) : ModelStoreResult
}

/**
 * Fetches a [ModelSpec]'s archive, verifies it against the hash pinned
 * in this repository, extracts it into app-internal storage, and hands
 * the provider a filesystem path.
 *
 * This is the download half of Model Delivery (ADR-0010): the Silero
 * VAD is bundled in app assets, and this store supplies the ~45 MB
 * streaming ASR model on first launch. It exists because sherpa-onnx
 * ships no runtime downloader — `docs/providers/sherpa-onnx.md` is
 * explicit that the fetch/verify/extract code is the consumer's to
 * write.
 *
 * The store is deliberately free of Android classes: it takes its root
 * directory as a [File], so the whole thing is unit-testable and the
 * app passes `context.noBackupFilesDir` — a ~45 MB model must never
 * enter Auto Backup, whose quota it would blow.
 *
 * Layout under [root]:
 * - `<root>/<spec.name>/` — the extracted model: present and complete, or absent
 * - `<root>/.<spec.name>.downloading/` — staging; a crashed run's remains
 *   are deleted on the next entry to [ensureModel]
 * - `<root>/<spec.name>.tar.bz2` — the downloaded archive, deleted once
 *   extraction succeeds
 *
 * The provider's seam is [modelDirectory] and [modelFiles]: pass a null
 * `AssetManager` to sherpa-onnx's recognizer constructors and point the
 * model config at the files this store hands over. That is the same
 * constructor argument the bundled path uses with a non-null
 * `AssetManager` — the nullable argument sherpa-onnx names as the
 * decision point for downloaded-vs-bundled — so "assets or downloaded"
 * is one boolean at the call site, and the provider (ticket 42) needs
 * no sherpa-specific branching to support both.
 */
class ModelStore(
    private val root: File,
    private val spec: ModelSpec,
    private val downloader: ArchiveDownloader = HttpUrlConnectionDownloader(),
) {
    private val modelDir = File(root, spec.name)
    private val stagingDir = File(root, ".${spec.name}.downloading")
    private val archiveFile = File(root, "${spec.name}.tar.bz2")
    private val extractor = ArchiveExtractor()

    /** Serializes downloads, so concurrent callers cannot fetch twice. */
    private val mutex = Mutex()

    /**
     * Whether the model is extracted and complete.
     *
     * This is the truth `SherpaOnnxProvider.health()` reports as
     * `MODEL_MISSING` (ADR-0001): with a downloaded default model,
     * absence is the common first-run state, and its fix is a download
     * prompt rather than a retry — so it must be distinguishable from
     * every other health value from the first launch onward.
     */
    fun isModelPresent(): Boolean = spec.requiredEntries.all { entry ->
        File(modelDir, spec.relativePath(entry)).isFile
    }

    /**
     * The extracted model directory, or null when the model is absent —
     * the null *is* the `MODEL_MISSING` signal, and the caller is
     * expected to offer the download (ticket 47), not to retry.
     */
    fun modelDirectory(): File? = if (isModelPresent()) modelDir else null

    /**
     * The extracted files this store guarantees, keyed by file name —
     * e.g. `"encoder-epoch-99-avg-1.int8.onnx"` — or an empty map when
     * the model is absent.
     *
     * The provider should build its model config from these rather than
     * retyping the file names: the names then live in exactly one place
     * (the spec), which is the same discipline ticket 32 applied to
     * prose — a filename written twice is a filename that drifts.
     */
    fun modelFiles(): Map<String, File> =
        if (!isModelPresent()) emptyMap()
        else spec.requiredEntries.associate { entry ->
            spec.baseName(entry) to File(modelDir, spec.relativePath(entry))
        }

    /**
     * Ensures the model is present, downloading and extracting it if
     * not. Idempotent: a second call on an extracted model downloads
     * nothing.
     *
     * Never throws for an operational failure — every one is a
     * [ModelStoreResult.Failed] whose reason names what happened,
     * because the caller's response to "no model" is the same UI either
     * way. Unexpected exceptions (programming errors) still propagate.
     *
     * Needs roughly the archive's size plus the extracted model's size
     * in free storage — about 175 MB for the V1 default, a 127 MB
     * archive beside a 45 MB extraction — because the archive is kept
     * until extraction succeeds.
     */
    suspend fun ensureModel(onProgress: ProgressCallback = { _, _ -> }): ModelStoreResult =
        mutex.withLock {
            if (isModelPresent()) return@withLock ModelStoreResult.AlreadyPresent
            withContext(Dispatchers.IO) {
                // A crashed run's staging is garbage; so is an incomplete
                // model directory — presence is all-or-nothing by definition.
                stagingDir.deleteRecursively()
                if (modelDir.exists()) modelDir.deleteRecursively()
                root.mkdirs()

                try {
                    downloader.download(spec.archiveUrl, archiveFile, onProgress)
                } catch (e: IOException) {
                    archiveFile.delete()
                    return@withContext ModelStoreResult.Failed("download failed: ${e.message}")
                }

                val actual = sha256Of(archiveFile)
                if (actual != spec.archiveSha256) {
                    archiveFile.delete()
                    return@withContext ModelStoreResult.Failed(
                        "integrity check failed for ${spec.archiveUrl}: " +
                            "expected sha-256 ${spec.archiveSha256}, got $actual"
                    )
                }

                try {
                    extractor.extract(archiveFile, stagingDir, spec)
                } catch (e: IOException) {
                    stagingDir.deleteRecursively()
                    archiveFile.delete()
                    return@withContext ModelStoreResult.Failed("extraction failed: ${e.message}")
                }

                // Extraction wrote only whitelisted entries; verify the
                // whitelist actually landed before the model becomes present.
                val extracted = File(stagingDir, spec.name)
                val missing = spec.requiredEntries.filterNot {
                    File(extracted, spec.relativePath(it)).isFile
                }
                if (missing.isNotEmpty()) {
                    stagingDir.deleteRecursively()
                    archiveFile.delete()
                    return@withContext ModelStoreResult.Failed(
                        "extraction produced an incomplete model: missing ${missing.sorted()}"
                    )
                }

                if (!extracted.renameTo(modelDir)) {
                    stagingDir.deleteRecursively()
                    archiveFile.delete()
                    return@withContext ModelStoreResult.Failed(
                        "could not move the extracted model into place"
                    )
                }
                stagingDir.deleteRecursively()
                archiveFile.delete()
                ModelStoreResult.Downloaded
            }
        }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            DigestInputStream(input, digest).use { digested ->
                val buffer = ByteArray(64 * 1024)
                while (digested.read(buffer) >= 0) {
                    // reading feeds the digest; the bytes themselves are
                    // never held
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
