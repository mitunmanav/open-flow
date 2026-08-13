package app.openflow.ai

import app.openflow.engine.ModelCapability

/**
 * Optional text enhancement hook. Product path uses [NoAI] only
 * (CleanupPipeline + VoiceCommands + styles — no model).
 */
interface TextAIProvider {
    val name: String

    val capability: ModelCapability
        get() = ModelCapability.noneBrain()

    suspend fun enhance(text: String, mode: String = "cleanup"): String
}

/** Identity. Zero network. Default forever until Mitun opts into a real model. */
object NoAI : TextAIProvider {
    override val name: String = "none"
    override val capability: ModelCapability = ModelCapability.noneBrain()

    override suspend fun enhance(text: String, mode: String): String = text
}
