package dev.openflow.dictation.providers.sherpa.modelstore

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

/**
 * Extraction's two load-bearing properties: that only the spec's
 * whitelisted entries are written (which is both the disk-space
 * decision and the path-traversal guard), and that a missing required
 * entry is a loud failure rather than a half-present model.
 */
class ArchiveExtractorTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val spec = ModelSpec(
        name = "test-model",
        archiveUrl = "https://example.test/test-model.tar.bz2",
        archiveSha256 = "0".repeat(64),
        requiredEntries = setOf("test-model/encoder.int8.onnx", "test-model/tokens.txt"),
    )

    @Test
    fun extractsOnlyTheEntriesTheSpecNames() {
        val archive = archiveWith(
            "test-model/encoder.int8.onnx" to "ENCODER",
            "test-model/tokens.txt" to "TOKENS",
            // present in the real archives, never loaded by the app
            "test-model/encoder.onnx" to "FP32",
            "test-model/test_wavs/0.wav" to "WAV",
            // a crafted archive's escape attempts
            "../escape.txt" to "ESCAPED",
            "/absolute/path.txt" to "ABSOLUTE",
        )
        val into = temp.newFolder("into")

        ArchiveExtractor().extract(archive, into, spec)

        assertEquals("ENCODER", File(into, "test-model/encoder.int8.onnx").readText())
        assertEquals("TOKENS", File(into, "test-model/tokens.txt").readText())
        // unlisted entries — including the escape attempts — were never
        // opened for writing
        assertFalse(File(into, "test-model/encoder.onnx").exists())
        assertFalse(File(into, "test-model/test_wavs").exists())
        assertFalse(File(into.parentFile, "escape.txt").exists())
        assertFalse(File(into, "absolute/path.txt").exists())
    }

    @Test
    fun aMissingRequiredEntryThrows() {
        val archive = archiveWith("test-model/encoder.int8.onnx" to "ENCODER")
        val into = temp.newFolder("into")

        try {
            ArchiveExtractor().extract(archive, into, spec)
            fail("expected a ModelArchiveException")
        } catch (e: ModelArchiveException) {
            // the message names what is missing, so the failure is
            // diagnosable from the result string alone
            assertTrue(e.message!!.contains("tokens.txt"))
        }
    }

    @Test
    fun aNestedRequiredEntryLandsAtItsRelativePath() {
        val nestedSpec = spec.copy(requiredEntries = setOf("test-model/sub/encoder.int8.onnx"))
        val archive = archiveWith("test-model/sub/encoder.int8.onnx" to "ENCODER")
        val into = temp.newFolder("into")

        ArchiveExtractor().extract(archive, into, nestedSpec)

        assertEquals("ENCODER", File(into, "test-model/sub/encoder.int8.onnx").readText())
    }

    private fun archiveWith(vararg entries: Pair<String, String>): File {
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
        return archive
    }
}
