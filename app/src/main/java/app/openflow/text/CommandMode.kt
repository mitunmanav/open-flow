package app.openflow.text

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider

/** Voice edit. Off = original. On = brain enhance(command). No history. */
object CommandMode {

    suspend fun apply(
        text: String,
        brainCommand: Boolean,
        brain: TextAIProvider = NoAI,
    ): String {
        if (!brainCommand) return text
        return brain.enhance(text, "command")
    }
}
