package app.openflow.bubble

/**
 * Bounded retry for overlay addView failures (transient token/permission
 * races at boot). After exhaustion the service stays up and posts a nudge.
 */
object OverlayRecoveryPolicy {
    val DELAYS_MS = listOf(1_000L, 5_000L, 15_000L)

    fun shouldRetry(attempt: Int): Boolean = attempt in DELAYS_MS.indices

    fun exhausted(attempt: Int): Boolean = attempt >= DELAYS_MS.size

    fun delayFor(attempt: Int): Long = DELAYS_MS[attempt.coerceIn(DELAYS_MS.indices)]
}
