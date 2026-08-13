package app.openflow.privacy

object RetentionPolicy {
    const val KEEP = "keep"
    const val WIPE_24H = "wipe_24h"
    const val NEVER_STORE = "never_store"

    /** Exact 24h. Tests fail if this drifts. */
    const val WIPE_WINDOW_MS = 24L * 60L * 60L * 1000L

    fun shouldPersist(policy: String): Boolean = policy != NEVER_STORE

    fun cutoffEpochMs(nowEpochMs: Long, policy: String): Long? =
        if (policy == WIPE_24H) nowEpochMs - WIPE_WINDOW_MS else null

    /** OpenFlow never writes audio bytes. Any policy. */
    @Suppress("UNUSED_PARAMETER")
    fun storesAudio(policy: String): Boolean = false
}
