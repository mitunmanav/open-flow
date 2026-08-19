package app.openflow.whisper

interface WhisperRuntime {
    fun isLoaded(): Boolean
    fun transcribe(samples: FloatArray): String
    fun release()
}

interface PcmSource {
    fun start() {}
    /** Stop mic and return 16 kHz mono float in -1..1. Empty if none. */
    fun take(): FloatArray
}
