package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.ChatJson

class AssemblyEar(
    apiKey: () -> String,
    socket: CloudSocket,
    hasMic: () -> Boolean = { true },
    pcm: PcmSource = PcmSource.None,
) : CloudEar(apiKey, socket, hasMic, pcm) {

    override fun connectUrl(languageTag: String): String =
        "wss://streaming.assemblyai.com/v3/ws?sample_rate=16000"

    override fun authHeaders(key: String): Map<String, String> =
        mapOf("Authorization" to key)

    override fun onSessionClose(session: CloudSession) {
        session.sendText("""{"type":"Terminate"}""")
    }

    override fun parse(message: String): EarUtterance? {
        val type = ChatJson.firstString(message, "type").orEmpty()
        if (type.isNotEmpty() && type != "Turn") return null
        val text = ChatJson.firstString(message, "transcript") ?: return null
        if (text.isBlank()) return null
        return EarUtterance(text, final = ChatJson.flag(message, "end_of_turn") == true)
    }
}
