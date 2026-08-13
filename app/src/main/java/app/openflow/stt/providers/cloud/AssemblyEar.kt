package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.ChatJson

class AssemblyEar(
    apiKey: () -> String,
    socket: CloudSocket,
    hasMic: () -> Boolean = { true },
) : CloudEar(apiKey, socket, hasMic) {

    override fun connectUrl(languageTag: String): String =
        "wss://streaming.assemblyai.com/v3/ws"

    override fun authHeaders(key: String): Map<String, String> =
        mapOf("Authorization" to key)

    override fun parse(message: String): EarUtterance? {
        val text = ChatJson.firstString(message, "transcript") ?: return null
        if (text.isBlank()) return null
        return EarUtterance(text, final = ChatJson.flag(message, "end_of_turn") == true)
    }
}
