package dev.openflow.dictation.providers.sherpa.modelstore

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Extracts a `.tar.bz2` model archive, writing only the entries a
 * [ModelSpec] names.
 *
 * Two decisions live here, and both are consequences of the archive's
 * measured shape (ticket 46):
 *
 * - **Selective extraction.** The `asr-models` archives carry fp32 and
 *   int8 variants plus test waves; OpenFlow loads the int8 trio and
 *   `tokens.txt`. Writing only the spec's entries keeps roughly 160 MB
 *   of unused weights off the user's device. The archive as a whole
 *   remains the unit of integrity — the ModelStore verifies the full
 *   archive's pinned SHA-256 before this class runs.
 * - **The whitelist is the traversal guard.** An entry is written only
 *   when its normalized name is in `spec.requiredEntries`, so a
 *   crafted archive cannot write outside the extraction root: a name
 *   with `..`, a leading `/`, or a backslash path is simply not in the
 *   set and is skipped. Nothing else is needed, because nothing else
 *   is ever opened for writing.
 */
internal class ArchiveExtractor {

    /**
     * Extracts [spec.requiredEntries] from the bzip2-compressed tar at
     * [archive] into [into], preserving each entry's archive-relative
     * path.
     *
     * @throws ModelArchiveException if the archive is not a readable
     *   bzip2 tar, or if any required entry is absent. An archive that
     *   verifies against the pinned hash but does not contain what the
     *   spec demands is a changed artifact, and must fail loudly rather
     *   than hand the provider a half-present model.
     */
    fun extract(archive: File, into: File, spec: ModelSpec) {
        val found = mutableSetOf<String>()
        BZip2CompressorInputStream(FileInputStream(archive)).use { bzip2 ->
            TarArchiveInputStream(bzip2).use { tar ->
                while (true) {
                    val entry = tar.nextTarEntry ?: break
                    if (entry.isDirectory) continue
                    val name = normalize(entry.name)
                    if (name !in spec.requiredEntries) continue
                    val target = File(into, name)
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output -> tar.copyTo(output) }
                    found += name
                }
            }
        }
        val missing = spec.requiredEntries - found
        if (missing.isNotEmpty()) {
            throw ModelArchiveException(
                "archive ${archive.name} did not contain required entries: ${missing.sorted()}"
            )
        }
    }

    /** Strips the `./` prefix and leading slashes a tar writer may add. */
    private fun normalize(entryName: String): String =
        entryName.removePrefix("./").trimStart('/')
}

/** An archive that cannot be read, or that does not contain what the spec demands. */
internal class ModelArchiveException(message: String) : IOException(message)
