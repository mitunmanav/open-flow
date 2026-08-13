package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.ChatJson

class OpenAiRealtimeEar(
    apiKey: () -> String,
    socket: CloudSocket,
    hasMic: () -> Boolean = { true },
) : CloudEar(apiKey, socket, hasMic) {

    override fun connectUrl(languageTag: String): String =
        "wss://api.openai.com/v1/realtime?intent=transcription"

    override fun authHeaders(key: String): Map<String, String> =
        mapOf("Authorization" to "Bearer $key")

    override fun parse(message: String): EarUtterance? {
        val type = ChatJson.firstString(message, "type").orEmpty()
        val delta = ChatJson.firstString(message, "delta")
        val transcript = ChatJson.firstString(message, "transcript")
        return when {
            type.contains("completed") && !transcript.isNullOrBlank() ->
                EarUtterance(transcript, final = true)
            type.contains("delta") && !delta.isNullOrBlank() ->
                EarUtterance(delta, final = false)
            else -> null
        }
    }
}
