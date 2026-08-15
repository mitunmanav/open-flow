package app.openflow.stt.providers.cloud

/** PCM16 LE mono helpers for cloud ears (mic = 16 kHz; OpenAI Realtime wants 24 kHz). */
object PcmResample {
    fun upsample16kTo24k(pcm16le: ByteArray): ByteArray {
        val nIn = pcm16le.size / 2
        if (nIn == 0) return ByteArray(0)
        val nOut = nIn * 3 / 2
        val out = ByteArray(nOut * 2)
        var o = 0
        var i = 0
        while (i < nIn && o < nOut) {
            writeShort(out, o++, readShort(pcm16le, i))
            if (o >= nOut) break
            val next = if (i + 1 < nIn) readShort(pcm16le, i + 1) else readShort(pcm16le, i)
            val mid = ((readShort(pcm16le, i).toInt() + next.toInt()) / 2).toShort()
            writeShort(out, o++, mid)
            i++
            if (i < nIn && o < nOut) {
                writeShort(out, o++, readShort(pcm16le, i))
                i++
            }
        }
        return out
    }

    private fun readShort(buf: ByteArray, index: Int): Short {
        val o = index * 2
        return ((buf[o].toInt() and 0xff) or (buf[o + 1].toInt() shl 8)).toShort()
    }

    private fun writeShort(buf: ByteArray, index: Int, value: Short) {
        val o = index * 2
        buf[o] = (value.toInt() and 0xff).toByte()
        buf[o + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }
}
