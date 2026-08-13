package app.openflow.engine

data class EarOutbound(val audioLeaves: Boolean)

object SendPolicy {
    private val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val phone = Regex("""\d{10,}""")

    /** This utterance only. No history, dict, or snippets. */
    fun forBrain(text: String): String {
        val stripped = text.replace(email, "").replace(phone, "")
        return stripped.replace(Regex("""\s+"""), " ").trim()
    }

    fun audioMustLeave(earNeedsNet: Boolean): Boolean = earNeedsNet

    /** Cloud ears send PCM. No redaction. */
    fun forEar(cloud: Boolean): EarOutbound = EarOutbound(audioLeaves = cloud)
}
