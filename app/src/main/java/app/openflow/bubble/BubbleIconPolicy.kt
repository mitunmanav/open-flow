package app.openflow.bubble

import java.io.File

object BubbleIconPolicy {
    const val FILE_NAME = "bubble_icon"

    fun localFile(filesDir: File): File = File(filesDir, FILE_NAME)

    fun validUri(raw: String): Boolean {
        val s = raw.trim()
        if (s.startsWith("content:") && s.length > "content:".length) return true
        if (!s.startsWith("file:")) return false
        return s.substringAfterLast('/').substringBefore('?') == FILE_NAME
    }

    fun useColorFilter(raw: String): Boolean = !validUri(raw)

    fun decodeSampleSize(srcW: Int, srcH: Int, maxPx: Int = 256): Int {
        if (srcW <= 0 || srcH <= 0 || maxPx <= 0) return 1
        var sample = 1
        while (srcW / sample > maxPx || srcH / sample > maxPx) {
            sample *= 2
        }
        return sample
    }
}
