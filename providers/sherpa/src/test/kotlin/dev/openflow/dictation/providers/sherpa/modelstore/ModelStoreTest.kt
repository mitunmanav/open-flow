package dev.openflow.dictation.providers.sherpa.modelstore

import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * The ModelStore's behaviour against a fake downloader and real (tiny)
 * tar.bz2 archives: the download/verify/extract/hand-over path, and
 * every way it must fail closed.
 */
class ModelStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val encoderEntry = "test-model/encoder-epoch-99-avg-1.int8.onnx"
    private val tokensEntry = "test-model/tokens.txt"

    @Test
    fun downloadsVerifiesAndExtractsWhenAbsent() {
        val archive = archiveWith(
            encoderEntry to "ENCODER",
            tokensEntry to "TOKENS",
            // the real archive carries entries the spec does not name —
            // fp32 variants, test waves; they must not reach the device
            "test-model/encoder-epoch-99-avg-1.onnx" to "FP32",
        )
        val downloader = FakeDownloader(archive)
        val root = temp.newFolder("root")
        val store = ModelStore(root, spec(archive), downloader)

        assertFalse(store.isModelPresent())
        assertNull(store.modelDirectory()) // the MODEL_MISSING signal

        val result = runBlocking { store.ensureModel() }

        assertEquals(ModelStoreResult.Downloaded, result)
        assertTrue(store.isModelPresent())
        assertEquals(File(root, "test-model"), store.modelDirectory())
        val files = store.modelFiles()
        assertEquals(
            setOf("encoder-epoch-99-avg-1.int8.onnx", "tokens.txt"),
            files.keys
        )
        assertEquals("ENCODER", files["encoder-epoch-99-avg-1.int8.onnx"]?.readText())
        assertEquals("TOKENS", files["tokens.txt"]?.readText())
        assertEquals(1, downloader.calls)
        // the archive and the staging directory are cleaned up, not left
        // beside the model
        assertFalse(File(root, "test-model.tar.bz2").exists())
        assertFalse(File(root, ".test-model.downloading").exists())
    }

    @Test
    fun aTamperedArchiveFailsClosed() {
        val archive = archiveWith(encoderEntry to "ENCODER", tokensEntry to "TOKENS")
        val tampered = archive.copyOf().also { bytes ->
            bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 0xFF).toByte()
        }
        val downloader = FakeDownloader(tampered)
        val root = temp.newFolder("root")
        val store = ModelStore(root, spec(archive), downloader)

        val result = runBlocking { store.ensureModel() }

        assertTrue(result is ModelStoreResult.Failed)
        assertTrue((result as ModelStoreResult.Failed).reason.contains("integrity check failed"))
        assertFalse(store.isModelPresent())
        assertNull(store.modelDirectory())
        // the tampered bytes are discarded, not left beside the model
        assertFalse(File(root, "test-model.tar.bz2").exists())
    }

    @Test
    fun anExtractedModelIsNotDownloadedAgain() {
        val archive = archiveWith(encoderEntry to "ENCODER", tokensEntry to "TOKENS")
        val downloader = FakeDownloader(archive)
        val store = ModelStore(temp.newFolder("root"), spec(archive), downloader)

        runBlocking { store.ensureModel() }
        val result = runBlocking { store.ensureModel() }

        assertEquals(ModelStoreResult.AlreadyPresent, result)
        assertEquals(1, downloader.calls)
    }

    @Test
    fun aFailedDownloadReportsWhy() {
        val archive = archiveWith(encoderEntry to "ENCODER", tokensEntry to "TOKENS")
        val downloader = FakeDownloader(archive, failure = IOException("offline"))
        val store = ModelStore(temp.newFolder("root"), spec(archive), downloader)

        val result = runBlocking { store.ensureModel() }

        assertEquals("download failed: offline", (result as ModelStoreResult.Failed).reason)
        assertFalse(store.isModelPresent())
    }

    @Test
    fun anArchiveMissingARequiredEntryFailsClosed() {
        // the archive verifies — the hash matches — but lacks tokens.txt
        val archive = archiveWith(encoderEntry to "ENCODER")
        val root = temp.newFolder("root")
        val store = ModelStore(root, spec(archive), FakeDownloader(archive))

        val result = runBlocking { store.ensureModel() }

        assertTrue((result as? ModelStoreResult.Failed)?.reason?.contains("extraction") == true)
        assertFalse(store.isModelPresent())
        assertFalse(File(root, "test-model").exists())
    }

    @Test
    fun staleStagingAndPartialModelsAreReplaced() {
        val archive = archiveWith(encoderEntry to "ENCODER", tokensEntry to "TOKENS")
        val root = temp.newFolder("root")
        // a crashed run's staging, and a half-written model directory
        File(root, ".test-model.downloading").mkdirs()
        File(root, ".test-model.downloading/junk").writeText("junk")
        File(root, "test-model").mkdirs()
        File(root, "test-model/tokens.txt").writeText("half")

        val store = ModelStore(root, spec(archive), FakeDownloader(archive))
        val result = runBlocking { store.ensureModel() }

        assertEquals(ModelStoreResult.Downloaded, result)
        assertTrue(store.isModelPresent())
        assertEquals(
            "ENCODER",
            store.modelFiles()["encoder-epoch-99-avg-1.int8.onnx"]?.readText()
        )
        assertFalse(File(root, ".test-model.downloading").exists())
    }

    /** Builds a real bzip2-compressed tar with the given entries. */
    private fun archiveWith(vararg entries: Pair<String, String>): ByteArray {
        val archive = temp.newFile("archive.tar.bz2")
        BZip2CompressorOutputStream(FileOutputStream(archive)).use { bzip2 ->
            TarArchiveOutputStream(bzip2).use { tar ->
                for ((name, content) in entries) {
                    val data = content.toByteArray()
                    val entry = TarArchiveEntry(name)
                    entry.size = data.size.toLong()
                    tar.putArchiveEntry(entry)
                    tar.write(data)
                    tar.closeArchiveEntry()
                }
            }
        }
        return archive.readBytes()
    }

    private fun spec(archive: ByteArray): ModelSpec = ModelSpec(
        name = "test-model",
        archiveUrl = "https://example.test/test-model.tar.bz2",
        archiveSha256 = sha256Of(archive),
        requiredEntries = setOf(encoderEntry, tokensEntry),
    )

    private fun sha256Of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private class FakeDownloader(
        private val bytes: ByteArray,
        private val failure: IOException? = null,
    ) : ArchiveDownloader {
        var calls = 0
            private set

        override suspend fun download(
            url: String,
            destination: File,
            onProgress: ProgressCallback,
        ) {
            calls++
            failure?.let { throw it }
            destination.writeBytes(bytes)
        }
    }
}
