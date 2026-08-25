package app.openflow.bubble

/**
 * Adaptive tick cadence for the bubble pulse loop.
 *
 * Fast only while something moves (listening, chips, drag). Once idle and
 * compacted, the loop drops to a slow heartbeat so the service sleeps
 * instead of waking the CPU every second all day.
 */
object PulseSchedule {
    const val LISTENING_MS = 66L
    const val CHIPS_MS = 250L
    const val DRAG_MS = 500L
    const val IDLE_HEARTBEAT_MS = 30_000L

    fun nextDelay(
        listening: Boolean,
        chipsVisible: Boolean,
        dragging: Boolean,
        idleMs: Long,
    ): Long = when {
        listening -> LISTENING_MS
        chipsVisible -> CHIPS_MS
        dragging -> DRAG_MS
        idleMs < IdleShrink.THRESHOLD_MS ->
            // One-shot: wake exactly at the idle-shrink boundary, then sleep.
            (IdleShrink.THRESHOLD_MS - idleMs).coerceAtMost(IDLE_HEARTBEAT_MS)
        else -> IDLE_HEARTBEAT_MS
    }
}
