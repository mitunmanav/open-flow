package app.openflow.stt.providers.cloud

/** Hardware PCM start gates. Fail closed — never pretend the mic is live. */
object PcmStartPolicy {
    fun bufferOk(minBuffer: Int): Boolean = minBuffer > 0
    fun recordOk(initialized: Boolean): Boolean = initialized
}
