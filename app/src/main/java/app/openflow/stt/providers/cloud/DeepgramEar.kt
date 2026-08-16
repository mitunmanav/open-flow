package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.ChatJson

class DeepgramEar(
    apiKey: () -> String,
    socket: CloudSocket,
    hasMic: () -> Boolean = { true },
    pcm: PcmSource = PcmSource.None,
) : CloudEar(apiKey, socket, hasMic, pcm) {

    override fun connectUrl(languageTag: String): String {
        val lang = languageTag.trim().ifBlank { "en-US" }
        return "wss://api.deepgram.com/v1/listen" +
            "?model=nova-2&encoding=linear16&sample_rate=16000&channels=1" +
            "&interim_results=true&punctuate=true&smart_format=true" +
            "&language=$lang"
    }

    override fun authHeaders(key: String): Map<String, String> =
        mapOf("Authorization" to "Token $key")

    override fun onSessionClose(session: CloudSession) {
        session.sendText("""{"type":"CloseStream"}""")
    }

    override fun parse(message: String): EarUtterance? {
        val text = ChatJson.firstString(message, "transcript") ?: return null
        if (text.isBlank()) return null
        return EarUtterance(text, final = ChatJson.flag(message, "is_final") == true)
    }
}
