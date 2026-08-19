package app.openflow.stt.providers.cloud

interface PcmSource {
    /** @return false if the mic did not start. Callers must not claim listening. */
    fun start(onChunk: (ByteArray) -> Unit): Boolean
    fun stop()

    companion object {
        val None: PcmSource = object : PcmSource {
            override fun start(onChunk: (ByteArray) -> Unit): Boolean = true
            override fun stop() = Unit
        }
    }
}
