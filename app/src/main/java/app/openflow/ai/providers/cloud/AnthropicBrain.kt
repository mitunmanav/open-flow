package app.openflow.ai.providers.cloud

import app.openflow.ai.TextAIProvider

class AnthropicBrain(
    private val apiKey: () -> String,
    private val model: String,
    private val http: CloudHttp,
) : TextAIProvider {

    override val name: String = "anthropic"

    val rewrite: Boolean = true
    val commandMode: Boolean = true
    val needsNet: Boolean = true
    val audioLeavesDevice: Boolean = true

    override suspend fun enhance(text: String, mode: String): String {
        val key = apiKey().trim()
        if (key.isEmpty()) return text
        val outbound = CloudMinimize.forBrain(text).ifEmpty { text }
        val system = if (mode == "command") outbound else CLEAN
        val user = outbound
        val body = """{"model":"${ChatJson.escape(model)}","max_tokens":1024,"system":"${ChatJson.escape(system)}","messages":[{"role":"user","content":"${ChatJson.escape(user)}"}]}"""
        val headers = mapOf(
            "x-api-key" to key,
            "anthropic-version" to "2023-06-01",
            "Content-Type" to "application/json",
        )
        return try {
            val raw = http.post(MESSAGES, headers, body)
            ChatJson.firstString(raw, "text")?.takeIf { it.isNotBlank() } ?: text
        } catch (e: Exception) {
            android.util.Log.w("AnthropicBrain", "enhance request failed, returning raw text", e)
            text
        }
    }

    companion object {
        private const val MESSAGES = "https://api.anthropic.com/v1/messages"
        private const val CLEAN = "Clean dictation. do not invent facts."
    }
}
