package app.openflow.bubble

/** Copy last transcript chip after a successful insert. */
object CopyChip {
    const val VISIBLE_MS = 10_000L

    fun shouldShow(ageMs: Long, listening: Boolean): Boolean =
        !listening && ageMs >= 0L && ageMs < VISIBLE_MS
}
