package app.openflow.whisper

interface WhisperRuntime {
    fun isLoaded(): Boolean
    fun preload() {}
    fun transcribe(samples: FloatArray): String
    fun release()
}

interface PcmSource {
    fun start(onSamples: (FloatArray) -> Unit = {}) {}
    /** Stop mic and return leftover 16 kHz mono float in -1..1. Empty if none. */
    fun take(): FloatArray
}
