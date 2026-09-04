package app.openflow.engine

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import app.openflow.stt.SpeechEngine

/**
 * Factories for the picked ear and brain.
 *
 * Lookup contract (honest about wiring):
 * - `ear(id)` / `brain(id)` return `null` when no factory is registered for [id].
 * - When a factory IS registered but throws (e.g. cloud ear without an API key),
 *   the registry falls back to `fallbackEar` / `fallbackBrain` so the app still
 *   works on system STT / no-AI.
 *
 * Callers that want a non-null instance should resolve their id through
 * [EarGate] (for ears) or `EarGate.resolveBrain` (for brains) before lookup —
 * the gate keeps stale preferences from reaching this registry.
 */
class ProviderRegistry(
    private val fallbackEar: () -> SpeechEngine,
    private val fallbackBrain: () -> TextAIProvider = { NoAI },
) {
    private val ears = mutableMapOf<EarId, () -> SpeechEngine>()
    private val brains = mutableMapOf<BrainId, () -> TextAIProvider>()

    fun registerEar(id: EarId, factory: () -> SpeechEngine) {
        ears[id] = factory
    }

    fun registerBrain(id: BrainId, factory: () -> TextAIProvider) {
        brains[id] = factory
    }

    fun ear(id: EarId): SpeechEngine? = firstWorking(ears[id], fallbackEar)

    fun ear(id: String): SpeechEngine? = ear(ProviderId.parseEar(id))

    fun brain(id: BrainId): TextAIProvider? = firstWorking(brains[id], fallbackBrain)

    fun brain(id: String): TextAIProvider? = brain(ProviderId.parseBrain(id))

    private fun <T> firstWorking(preferred: (() -> T)?, fallback: () -> T): T? {
        if (preferred == null) return null
        return try {
            preferred()
        } catch (_: Exception) {
            try {
                fallback()
            } catch (_: Exception) {
                null
            }
        }
    }
}
