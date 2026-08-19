package app.openflow.text

/**
 * After cleanup, empty output is only legal for explicit wipe or filler-only speech.
 * Real words must be recovered.
 */
object KeepContentPolicy {
    enum class Kind { KEEP_CLEAN, ALLOW_EMPTY, RECOVER }

    fun decide(clean: String, explicitWipe: Boolean, hasContentWords: Boolean): Kind {
        if (clean.isNotBlank()) return Kind.KEEP_CLEAN
        if (explicitWipe) return Kind.ALLOW_EMPTY
        if (!hasContentWords) return Kind.ALLOW_EMPTY
        return Kind.RECOVER
    }
}
