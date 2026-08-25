package app.openflow.audio

/** Heap cap for the retry-WAV buffer. Head of session kept; overflow dropped. */
object CaptureCap {
    /** ≈4 min of 16 kHz mono 16-bit audio. */
    const val DEFAULT_MAX_BYTES: Long = 8L * 1024 * 1024

    fun admit(retained: Long, incoming: Int, maxBytes: Long = DEFAULT_MAX_BYTES): Boolean =
        retained + incoming <= maxBytes
}
