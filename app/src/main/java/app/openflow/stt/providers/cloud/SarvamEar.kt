package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.ChatJson
import java.util.Base64

/** Sarvam STT: wss speech-to-text/ws, base64 JSON audio, api-subscription-key. */
class SarvamEar(
    apiKey: () -> String,
    socket: CloudSocket,
    private val mode: String = "transcribe",
    hasMic: () -> Boolean = { true },
    pcm: PcmSource = PcmSource.None,
) : CloudEar(apiKey, socket, hasMic, pcm) {

    override fun connectUrl(languageTag: String): String {
        val lang = sarvamLanguage(languageTag)
        val m = mode.ifBlank { "transcribe" }
        return "wss://api.sarvam.ai/speech-to-text/ws" +
            "?model=saaras:v3" +
            "&mode=$m" +
            "&language-code=$lang" +
            "&sample_rate=16000" +
            "&input_audio_codec=pcm_s16le"
    }

    override fun authHeaders(key: String): Map<String, String> =
        mapOf("api-subscription-key" to key)

    override fun writeAudio(session: CloudSession, pcm: ByteArray) {
        val b64 = Base64.getEncoder().encodeToString(pcm)
        session.sendText(
            """{"audio":{"data":"$b64","sample_rate":"16000","encoding":"audio/wav"}}""",
        )
    }

    override fun onSessionClose(session: CloudSession) {
        session.sendText("""{"type":"flush"}""")
    }

    override fun parse(message: String): EarUtterance? {
        val responseType = ChatJson.firstString(message, "type")
        if (responseType == "data") {
            val text = ChatJson.firstString(message, "transcript") ?: return null
            if (text.isBlank()) return null
            return EarUtterance(text, final = true)
        }
        if (responseType == "error" || responseType == "events") return null
        val event = ChatJson.firstString(message, "event").orEmpty()
        val text = ChatJson.firstString(message, "text") ?: return null
        if (text.isBlank()) return null
        return when {
            event.contains("final") -> EarUtterance(text, final = true)
            event.contains("partial") -> EarUtterance(text, final = false)
            else -> null
        }
    }

    companion object {
        /** Sarvam expects en-IN / hi-IN style tags, not en-US. */
        fun sarvamLanguage(languageTag: String): String {
            val t = languageTag.trim().ifBlank { return "en-IN" }
            return when {
                t.equals("en-US", ignoreCase = true) || t.equals("en", ignoreCase = true) -> "en-IN"
                else -> t
            }
        }
    }
}
