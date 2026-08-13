package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.ChatJson

class SarvamEar(
    apiKey: () -> String,
    socket: CloudSocket,
    private val mode: String = "transcribe",
    hasMic: () -> Boolean = { true },
) : CloudEar(apiKey, socket, hasMic) {

    override fun connectUrl(languageTag: String): String {
        val lang = languageTag.ifBlank { "en-IN" }
        val m = mode.ifBlank { "transcribe" }
        return "wss://api.sarvam.ai/speech-to-text-realtime/ws" +
            "?model=saaras:v3-realtime" +
            "&mode=$m" +
            "&language_code=$lang"
    }

    override fun authHeaders(key: String): Map<String, String> =
        mapOf("api-subscription-key" to key)

    override fun parse(message: String): EarUtterance? {
        val event = ChatJson.firstString(message, "event").orEmpty()
        val text = ChatJson.firstString(message, "text") ?: return null
        if (text.isBlank()) return null
        return when {
            event.contains("final") -> EarUtterance(text, final = true)
            event.contains("partial") -> EarUtterance(text, final = false)
            else -> null
        }
    }

}
