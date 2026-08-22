package app.openflow.whisper

import java.io.File

internal class WhisperLib private constructor() {
    companion object {
        init {
            val cpu = cpuInfo().orEmpty()
            if (cpu.contains("fphp")) {
                System.loadLibrary("whisper_v8fp16_va")
            } else {
                System.loadLibrary("whisper")
            }
        }

        external fun initContext(modelPath: String): Long
        external fun freeContext(contextPtr: Long)
        external fun fullTranscribe(
            contextPtr: Long,
            numThreads: Int,
            audioData: FloatArray,
            audioCtx: Int,
        )
        external fun getTextSegmentCount(contextPtr: Long): Int
        external fun getTextSegment(contextPtr: Long, index: Int): String
        external fun getSystemInfo(): String

        private fun cpuInfo(): String? = try {
            File("/proc/cpuinfo").readText()
        } catch (_: Exception) {
            null
        }
    }
}
