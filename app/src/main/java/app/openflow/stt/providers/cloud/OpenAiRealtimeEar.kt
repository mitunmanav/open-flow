package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.ChatJson
import app.openflow.stt.LanguagePolicy
import java.util.Base64

class OpenAiRealtimeEar(
    apiKey: () -> String,
    socket: CloudSocket,
    hasMic: () -> Boolean = { true },
    pcm: PcmSource = PcmSource.None,
) : CloudEar(apiKey, socket, hasMic, pcm) {

    private var iso639: String = "en"

    override fun connectUrl(languageTag: String): String {
        iso639 = LanguagePolicy.iso639(languageTag)
        return "wss://api.openai.com/v1/realtime?intent=transcription"
    }

    override fun authHeaders(key: String): Map<String, String> =
        mapOf(
            "Authorization" to "Bearer $key",
            "OpenAI-Beta" to "realtime=v1",
        )

    override fun onSessionOpen(session: CloudSession) {
        // Docs: session.update + type=transcription; PCM 24 kHz; gpt-live-transcribe + languages[].
        session.sendText(
            """{"type":"session.update","session":{"type":"transcription","audio":{"input":{"format":{"type":"audio/pcm","rate":24000},"transcription":{"model":"gpt-live-transcribe","languages":["$iso639"]},"turn_detection":{"type":"server_vad"}}}}}""",
        )
    }

    override fun writeAudio(session: CloudSession, pcm: ByteArray) {
        val at24k = PcmResample.upsample16kTo24k(pcm)
        val b64 = Base64.getEncoder().encodeToString(at24k)
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
