package app.openflow.ai.providers.ondevice

import app.openflow.ai.TextAIProvider
import java.io.File

/** On-phone brain stub. No packed LLM this slice — enhance is identity until a file + runtime exist. */
class OnDeviceBrain(
    private val modelFile: File? = null,
) : TextAIProvider {

    override val name: String = "on_phone"

    val rewrite: Boolean
        get() = modelFile?.isFile == true

    override suspend fun enhance(text: String, mode: String): String = text
}
