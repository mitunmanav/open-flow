package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.ChatJson
import java.util.Base64

class OpenAiRealtimeEar(
    apiKey: () -> String,
    socket: CloudSocket,
    hasMic: () -> Boolean = { true },
    pcm: PcmSource = PcmSource.None,
) : CloudEar(apiKey, socket, hasMic, pcm) {

    override fun connectUrl(languageTag: String): String =
        "wss://api.openai.com/v1/realtime?intent=transcription"

    override fun authHeaders(key: String): Map<String, String> =
        mapOf(
            "Authorization" to "Bearer $key",
            "OpenAI-Beta" to "realtime=v1",
        )

    override fun onSessionOpen(session: CloudSession) {
        session.sendText(
            """{"type":"session.update","session":{"input_audio_format":"pcm16"}}""",
        )
    }

    override fun writeAudio(session: CloudSession, pcm: ByteArray) {
        val b64 = Base64.getEncoder().encodeToString(pcm)
        session.sendText(
            """{"type":"input_audio_buffer.append","audio":"$b64"}""",
        )
    }

    override fun onSessionClose(session: CloudSession) {
        session.sendText("""{"type":"input_audio_buffer.commit"}""")
    }

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
