package app.openflow.privacy

object RetentionPolicy {
    fun shouldPersist(policy: String): Boolean = policy != "never_store"

    fun cutoffEpochMs(nowEpochMs: Long, policy: String): Long? =
        if (policy == "wipe_24h") nowEpochMs - 24L * 60L * 60L * 1000L else null
}
