package app.openflow.stt.providers.ondevice

import java.io.File

fun interface ModelDownloader {
    fun download(url: String, dest: File)
}

class ModelStore(
    private val modelsDir: File,
    private val downloader: ModelDownloader,
    private val minReadyBytes: Long = 1_000_000L,
) {
    fun file(id: String, url: String = ""): File = File(modelsDir, fileName(id, url))

    fun isReady(id: String, url: String = ""): Boolean {
        val f = file(id, url)
        return f.isFile && f.length() >= minReadyBytes
    }

    fun ensure(id: String, url: String): File {
        val dest = file(id, url)
        if (dest.isFile && dest.length() > 0L) return dest
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        downloader.download(url, tmp)
        if (!tmp.isFile || tmp.length() <= 0L) error("download empty")
        if (!tmp.renameTo(dest)) {
            dest.delete()
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        return dest
    }

    private fun fileName(id: String, url: String): String {
        val fromUrl = url.substringAfterLast('/').substringBefore('?')
        return fromUrl.ifBlank { "ggml-$id.bin" }
    }
}
