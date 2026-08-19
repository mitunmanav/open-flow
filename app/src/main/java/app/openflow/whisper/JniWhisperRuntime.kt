package app.openflow.whisper

import java.io.File

class JniWhisperRuntime(
    private val model: File,
    private val threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
) : WhisperRuntime {
    private var ptr: Long = 0L

    override fun isLoaded(): Boolean = ptr != 0L || model.isFile

    @Synchronized
    override fun transcribe(samples: FloatArray): String {
        if (ptr == 0L) {
            ptr = WhisperLib.initContext(model.absolutePath)
            if (ptr == 0L) return ""
        }
        if (samples.isEmpty()) return ""
        WhisperLib.fullTranscribe(ptr, threads, samples)
        val n = WhisperLib.getTextSegmentCount(ptr)
        return buildString {
            for (i in 0 until n) {
                append(WhisperLib.getTextSegment(ptr, i))
            }
        }.trim()
    }

    @Synchronized
    override fun release() {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0L
        }
    }
}
