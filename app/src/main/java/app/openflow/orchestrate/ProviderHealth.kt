package app.openflow.orchestrate

enum class HealthState { HEALTHY, DEGRADED, UNAVAILABLE }

class ProviderHealth(
    private val failThreshold: Int = 3,
    private val cooldownMs: Long = 60_000L,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private data class Entry(
        var consecutiveFails: Int = 0,
        var unavailableSince: Long? = null,
    )

    private val entries = mutableMapOf<String, Entry>()

    fun recordSuccess(id: String) {
        entries.remove(id)
    }

    fun recordFailure(id: String) {
        val entry = entries.getOrPut(id) { Entry() }
        entry.consecutiveFails++
        if (entry.consecutiveFails >= failThreshold) {
            entry.unavailableSince = clock()
        }
    }

    fun state(id: String): HealthState {
        val entry = entries[id] ?: return HealthState.HEALTHY
        if (entry.consecutiveFails >= failThreshold) {
            val since = entry.unavailableSince ?: return HealthState.UNAVAILABLE
            if (clock() - since < cooldownMs) {
                return HealthState.UNAVAILABLE
            }
            entries.remove(id)
            return HealthState.HEALTHY
        }
        return HealthState.DEGRADED
    }

    fun isAvailable(id: String): Boolean = state(id) != HealthState.UNAVAILABLE
}
