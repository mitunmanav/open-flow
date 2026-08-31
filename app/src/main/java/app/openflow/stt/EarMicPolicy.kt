package app.openflow.stt

/** Who may open AudioRecord. Whisper ear owns the mic; bubble WAV does not. */
object EarMicPolicy {
    fun bubbleCapturesWav(earId: String): Boolean =
        earId.trim().lowercase() != "on_phone"

    /** M3-A: when tee is active, system ear must NOT get WAV (platform owns mic). */
    fun teeEnabledFor(earId: String): Boolean {
        val id = earId.trim().lowercase()
        return id != "system"
    }

    /** M3-A aware: should this ear capture WAV via app? */
    fun shouldCaptureWav(earId: String, useTee: Boolean): Boolean {
        if (earId.trim().lowercase() == "on_phone") return false
        if (useTee && earId.trim().lowercase() == "system") return false
        return true
    }
}
