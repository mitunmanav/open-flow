package app.openflow.bubble

/** Copy last transcript chip after a successful insert. */
object CopyChip {
    const val VISIBLE_MS = 6_000L

    fun visibleMs(pref: String): Long = when (pref) {
        "3" -> 3_000L
        "10" -> 10_000L
        else -> 6_000L
    }

    fun shouldShow(
        ageMs: Long,
        listening: Boolean,
        visibleMs: Long = visibleMs("6"),
    ): Boolean = !listening && ageMs >= 0L && ageMs < visibleMs
}
