package app.openflow.orchestrate

import app.openflow.engine.EarGate

object SttRouter {
    private val cloudEars = setOf("openai", "deepgram", "assemblyai", "sarvam")

    fun pick(
        auto: Boolean,
        manualEarId: String,
        signals: RouteSignals,
        health: ProviderHealth,
        candidates: List<String> =
            listOf("system", "on_phone", "openai", "deepgram", "assemblyai", "sarvam"),
    ): RouteExplain {
        if (!auto) {
            return RouteExplain(EarGate.resolve(manualEarId), "manual")
        }

        val keyed = signals.keyedEars.map { it.trim().lowercase() }.toSet()
        val filtered =
            candidates.filter { id ->
                val normalized = id.trim().lowercase()
                if (!health.isAvailable(normalized)) return@filter false
                if (normalized in cloudEars) {
                    return@filter signals.online && normalized in keyed
                }
                // Phone speech (`system`) owns the on-device factory pref.
                // Never pick Whisper stub `on_phone` — EarGate.live stays false.
                EarGate.live(normalized)
            }

        val picked = filtered.firstOrNull()
        if (picked != null) {
            val normalized = picked.trim().lowercase()
            val reason =
                when {
                    normalized in cloudEars -> "cloud-keyed"
                    else -> "local-first"
                }
            return RouteExplain(normalized, reason)
        }
        return RouteExplain("system", "fallback-system")
    }
}
