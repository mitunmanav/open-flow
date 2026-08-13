package app.openflow.bubble

import app.openflow.ai.TextAIProvider
import app.openflow.text.Feature
import app.openflow.text.FeatureGate

/**
 * Surrounding field text for polish / brain.enhance.
 * Only when [Feature.FIELD_CONTEXT] would be on. Never for analytics.
 */
object FieldContext {

    fun on(brainRewrite: Boolean): Boolean =
        FeatureGate.can(Feature.FIELD_CONTEXT, brainRewrite = brainRewrite)

    fun surrounding(on: Boolean, fieldText: String): String =
        if (!on) "" else fieldText.trim()

    fun enhanceInput(spoken: String, surrounding: String): String {
        val said = spoken.trim()
        val around = surrounding.trim()
        if (around.isEmpty()) return said
        return "Field: $around\nSaid: $said"
    }

    fun wrapBrain(brain: TextAIProvider, surrounding: String): TextAIProvider {
        val around = surrounding.trim()
        if (around.isEmpty()) return brain
        return object : TextAIProvider {
            override val name: String = brain.name
            override val capability = brain.capability
            override suspend fun enhance(text: String, mode: String): String =
                brain.enhance(enhanceInput(text, around), mode)
        }
    }
}
