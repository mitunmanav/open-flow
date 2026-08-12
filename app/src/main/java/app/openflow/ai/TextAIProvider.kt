package app.openflow.ai

/**
 * Optional text enhancement hook. Product path uses [NoAI] only
 * (CleanupPipeline + VoiceCommands + styles — no model).
 */
interface TextAIProvider {
    val name: String

    suspend fun enhance(text: String, mode: String = "cleanup"): String
}

/** Identity. Zero network. Default forever until Mitun opts into a real model. */
object NoAI : TextAIProvider {
    override val name: String = "none"

    override suspend fun enhance(text: String, mode: String): String = text
}
