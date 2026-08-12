package app.openflow.bubble

enum class SessionPhase { NONE, WARN, STOP }

/** Wispr Android-style 5 minute session cap. */
object SessionGuard {
    const val WARN_MS = 270_000L
    const val STOP_MS = 300_000L

    fun phase(elapsedMs: Long): SessionPhase = when {
        elapsedMs >= STOP_MS -> SessionPhase.STOP
        elapsedMs >= WARN_MS -> SessionPhase.WARN
        else -> SessionPhase.NONE
    }
}
