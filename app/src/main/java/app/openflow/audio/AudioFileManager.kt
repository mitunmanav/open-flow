package app.openflow.audio

import android.content.Context
import java.io.File

/**
 * App-private storage manager for dictation session audio recordings (.m4a files).
 */
class AudioFileManager(private val baseDir: File) {

    constructor(context: Context) : this(File(context.filesDir, "audio").apply { mkdirs() })

    fun getAudioFile(sessionId: String): File {
        val safeId = sessionId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(baseDir, "$safeId.m4a")
    }

    fun hasAudio(sessionId: String): Boolean {
        val file = getAudioFile(sessionId)
        return file.exists() && file.length() > 0
    }

    fun deleteAudio(sessionId: String): Boolean {
        val file = getAudioFile(sessionId)
        return if (file.exists()) file.delete() else false
    }

    fun listAudioSessions(): List<String> {
        return baseDir.listFiles { file -> file.isFile && file.name.endsWith(".m4a") }
            ?.map { it.name.removeSuffix(".m4a") }
            ?: emptyList()
    }

    fun purgeAll(): Int {
        var count = 0
        baseDir.listFiles()?.forEach {
            if (it.isFile && it.delete()) count++
        }
        return count
    }
}
