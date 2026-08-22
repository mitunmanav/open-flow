package app.openflow.whisper

/** Whisper encoder frames. Default model ctx is 1500 = 30 s. */
object AudioCtx {
    const val SAMPLE_RATE = 16_000
    const val FRAMES_PER_SEC = 50
    const val MIN = 150
    const val MAX = 1500
    const val PAD = 16

    fun frames(nSamples: Int): Int {
        if (nSamples <= 0) return MIN
        val raw = (nSamples.toLong() * FRAMES_PER_SEC + SAMPLE_RATE - 1) / SAMPLE_RATE
        return (raw.toInt() + PAD).coerceIn(MIN, MAX)
    }
}
