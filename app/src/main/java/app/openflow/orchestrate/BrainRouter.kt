package app.openflow.orchestrate

import app.openflow.engine.EarGate

object BrainRouter {
    private val cloudBrains = setOf("openai", "anthropic", "grok", "gemini")

    fun pick(
        auto: Boolean,
        manualBrainId: String,
        signals: RouteSignals,
        health: ProviderHealth,
        looksLikeCommand: Boolean,
        textLen: Int,
        candidates: List<String> = listOf("none", "openai", "anthropic", "grok", "gemini"),
    ): RouteExplain {
        if (looksLikeCommand) {
            return RouteExplain("none", "command-local")
        }
        if (!auto) {
            return RouteExplain(EarGate.resolveBrain(manualBrainId), "manual")
        }
        if (textLen < 40) {
            return RouteExplain("none", "short-skip-brain")
        }

        val keyed = signals.keyedBrains.map { it.trim().lowercase() }.toSet()
        val picked =
            candidates.firstOrNull { id ->
                val normalized = id.trim().lowercase()
                if (normalized == "none" || normalized !in cloudBrains) return@firstOrNull false
                if (!health.isAvailable(normalized)) return@firstOrNull false
                signals.online && normalized in keyed
            }

        if (picked != null) {
            return RouteExplain(picked.trim().lowercase(), "cloud-keyed")
        }
        return RouteExplain("none", "fallback-none")
    }
}
