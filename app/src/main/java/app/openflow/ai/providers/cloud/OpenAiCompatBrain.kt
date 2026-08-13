package app.openflow.ai.providers.cloud

import app.openflow.ai.TextAIProvider

class OpenAiCompatBrain(
    private val id: String,
    private val apiKey: () -> String,
    private val model: String,
    private val baseUrl: String,
    private val http: CloudHttp,
) : TextAIProvider {

    override val name: String = id

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
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val body = chatBody(model, system, user)
        val headers = mapOf(
            "Authorization" to "Bearer $key",
            "Content-Type" to "application/json",
        )
        return try {
            val raw = http.post(url, headers, body)
            ChatJson.firstString(raw, "content")?.takeIf { it.isNotBlank() } ?: text
        } catch (_: Exception) {
            text
        }
    }

    private fun chatBody(model: String, system: String, user: String): String {
        val m = ChatJson.escape(model)
        val s = ChatJson.escape(system)
        val u = ChatJson.escape(user)
        return """{"model":"$m","messages":[{"role":"system","content":"$s"},{"role":"user","content":"$u"}]}"""
    }

    companion object {
        private const val CLEAN = "Clean dictation. do not invent facts."
    }
}
