package app.openflow.whisper

import android.util.Log
import app.openflow.BuildConfig
import java.io.File

class JniWhisperRuntime(
    private val model: File,
    private val threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 4),
) : WhisperRuntime {
    private var ptr: Long = 0L
    private var loggedSys = false

    override fun isLoaded(): Boolean = ptr != 0L || model.isFile

    @Synchronized
    override fun preload() {
        ensureContext()
    }

    @Synchronized
    override fun transcribe(samples: FloatArray): String {
        if (ensureContext() == 0L) return ""
        if (samples.isEmpty()) return ""
        val ctx = AudioCtx.frames(samples.size)
        val t0 = System.nanoTime()
        WhisperLib.fullTranscribe(ptr, threads, samples, ctx)
        val ms = (System.nanoTime() - t0) / 1_000_000L
        val audioMs = WhisperMetrics.audioMs(samples.size)
        val metrics = WhisperMetrics(loadMs = 0, transcribeMs = ms, audioMs = audioMs)
        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "transcribe_ms=$ms audio_ms=$audioMs rtf=${"%.3f".format(metrics.rtf)} " +
                    "samples=${samples.size} audio_ctx=$ctx threads=$threads",
            )
        }
        val n = WhisperLib.getTextSegmentCount(ptr)
        return buildString {
            for (i in 0 until n) {
                append(WhisperLib.getTextSegment(ptr, i))
            }
        }.trim()
    }

    private fun ensureContext(): Long {
        if (ptr == 0L) {
            val t0 = System.nanoTime()
            ptr = WhisperLib.initContext(model.absolutePath)
            val ms = (System.nanoTime() - t0) / 1_000_000L
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "load_ms=$ms path=${model.absolutePath} ptr=$ptr")
            }
            if (!loggedSys && ptr != 0L) {
                loggedSys = true
                if (BuildConfig.DEBUG) Log.i(TAG, "sys=${WhisperLib.getSystemInfo()}")
            }
        }
        return ptr
    }

    companion object {
        private const val TAG = "OpenFlowWhisper"
    }

    @Synchronized
    override fun release() {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0L
        }
    }
}
