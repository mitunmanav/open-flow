package app.openflow.stt.providers.cloud

interface PcmSource {
    fun start(onChunk: (ByteArray) -> Unit)
    fun stop()

    companion object {
        val None: PcmSource = object : PcmSource {
            override fun start(onChunk: (ByteArray) -> Unit) = Unit
            override fun stop() = Unit
        }
    }
}
