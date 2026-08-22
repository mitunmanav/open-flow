package app.openflow.whisper

data class WhisperMetrics(
    val loadMs: Long,
    val transcribeMs: Long,
    val audioMs: Long,
) {
    val rtf: Double get() = if (audioMs <= 0L) 0.0 else transcribeMs.toDouble() / audioMs

    companion object {
        fun audioMs(samples: Int, sampleRate: Int = 16_000): Long =
            if (sampleRate <= 0) 0L else samples.toLong() * 1000L / sampleRate
    }
}
