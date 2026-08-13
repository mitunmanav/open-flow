package app.openflow.engine

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import app.openflow.stt.SpeechEngine

/**
 * Factories for the picked ear and brain.
 * Missing or broken factory → system ear / [NoAI]. Never throws to callers.
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

    fun ear(id: EarId): SpeechEngine = firstWorking(ears[id], fallbackEar)

    fun ear(id: String): SpeechEngine = ear(ProviderId.parseEar(id))

    fun brain(id: BrainId): TextAIProvider = firstWorking(brains[id], fallbackBrain)

    fun brain(id: String): TextAIProvider = brain(ProviderId.parseBrain(id))

    private fun <T> firstWorking(preferred: (() -> T)?, fallback: () -> T): T {
        if (preferred != null) {
            try {
                return preferred()
            } catch (_: Exception) {
            }
        }
        return try {
            fallback()
        } catch (_: Exception) {
            fallback()
        }
    }
}
