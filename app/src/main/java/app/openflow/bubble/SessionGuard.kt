package app.openflow.bubble

enum class SessionPhase { NONE, WARN, STOP }

/** Wispr Android-style 5 minute session cap. */
object SessionGuard {
    const val WARN_MS = 270_000L
    const val STOP_MS = 300_000L

    fun phase(elapsedMs: Long): SessionPhase {
        val t = elapsedMs.coerceAtLeast(0L)
        return when {
            t >= STOP_MS -> SessionPhase.STOP
            t >= WARN_MS -> SessionPhase.WARN
            else -> SessionPhase.NONE
        }
    }

    /** Ms left before hard stop. Never negative. */
    fun remainingMs(elapsedMs: Long): Long =
        (STOP_MS - elapsedMs.coerceAtLeast(0L)).coerceAtLeast(0L)
}
