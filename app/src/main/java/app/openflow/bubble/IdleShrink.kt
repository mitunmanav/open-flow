package app.openflow.bubble

/** Visual compact after 5s idle. Does not change saved bubbleMode. */
object IdleShrink {
    const val THRESHOLD_MS = 5_000L

    fun shouldCompact(idleMs: Long, listening: Boolean, dragging: Boolean): Boolean =
        idleMs >= THRESHOLD_MS && !listening && !dragging
}
