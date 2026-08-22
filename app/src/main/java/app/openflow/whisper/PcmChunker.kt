package app.openflow.whisper

/** Fixed-size PCM windows. Never drops samples. Remainder stays until [flush]. */
class PcmChunker(private val chunkSamples: Int) {
    init {
        require(chunkSamples > 0)
    }

    private var buf = FloatArray(chunkSamples)
    private var n = 0

    fun push(samples: FloatArray): List<FloatArray> {
        if (samples.isEmpty()) return emptyList()
        val out = ArrayList<FloatArray>()
        var i = 0
        while (i < samples.size) {
            val take = minOf(chunkSamples - n, samples.size - i)
            System.arraycopy(samples, i, buf, n, take)
            n += take
            i += take
            if (n == chunkSamples) {
                out.add(buf.copyOf())
                buf = FloatArray(chunkSamples)
                n = 0
            }
        }
        return out
    }

    fun flush(): FloatArray {
        if (n == 0) return FloatArray(0)
        val rest = buf.copyOf(n)
        buf = FloatArray(chunkSamples)
        n = 0
        return rest
    }
}
