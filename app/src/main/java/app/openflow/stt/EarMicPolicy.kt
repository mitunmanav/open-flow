package app.openflow.stt

/** Who may open AudioRecord. Whisper ear owns the mic; bubble WAV does not. */
object EarMicPolicy {
    fun bubbleCapturesWav(earId: String): Boolean =
        earId.trim().lowercase() != "on_phone"
}
