package dev.openflow.dictation.providers.sherpa.modelstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Progress while downloading: [downloadedBytes] of [totalBytes], where
 * [totalBytes] is 0 when the server reports no content length. Called
 * per buffer, so a consumer that renders it should throttle.
 */
typealias ProgressCallback = (downloadedBytes: Long, totalBytes: Long) -> Unit

/**
 * Streams an archive to disk.
 *
 * An interface because the download is the one step whose failure modes
 * — offline, a 404 after an upstream release is deleted, a stalled CDN
 * — a test needs to reproduce on demand, and because the ModelStore
 * must be testable without the network.
 */
interface ArchiveDownloader {
    suspend fun download(url: String, destination: File, onProgress: ProgressCallback)
}

/**
 * The platform client: HTTPS to the pinned URL, streamed to disk in
 * 64 KB buffers so a 127 MB archive never sits in memory.
 *
 * The pinned SHA-256, not the transport, is the integrity guarantee —
 * but a non-200 still fails loudly rather than writing an HTML error
 * page to disk and letting it fail the hash check with a message that
 * blames the archive. HttpURLConnection follows GitHub's redirect to
 * its CDN by default for GET.
 */
class HttpUrlConnectionDownloader(
    private val connectTimeoutMillis: Int = 30_000,
    private val readTimeoutMillis: Int = 30_000,
) : ArchiveDownloader {

    override suspend fun download(
        url: String,
        destination: File,
        onProgress: ProgressCallback,
    ) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis
        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP $status downloading $url")
            }
            val totalBytes = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloadedBytes = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        onProgress(downloadedBytes, totalBytes)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
