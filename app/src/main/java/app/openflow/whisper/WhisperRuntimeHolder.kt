package app.openflow.whisper

import java.io.File

/** One native context. Create on first use. Free on switch/trim. */
class WhisperRuntimeHolder(
    private val factory: (File) -> WhisperRuntime,
) {
    private var file: File? = null
    private var runtime: WhisperRuntime? = null

    @Synchronized
    fun get(model: File): WhisperRuntime {
        val cur = runtime
        if (cur != null && file == model) return cur
        cur?.release()
        file = model
        val next = factory(model)
        runtime = next
        return next
    }

    @Synchronized
    fun release() {
        runtime?.release()
        runtime = null
        file = null
    }

    @Synchronized
    fun releaseIf(drop: Boolean) {
        if (drop) release()
    }
}
