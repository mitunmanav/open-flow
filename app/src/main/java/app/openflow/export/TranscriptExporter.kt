package app.openflow.export

import java.util.concurrent.TimeUnit

data class ExportSession(
    val id: String,
    val title: String,
    val createdAtEpochMs: Long,
    val durationMs: Long,
    val transcript: String,
    val languageTag: String
)

object TranscriptExporter {

    fun toTxt(session: ExportSession): String = buildString {
        appendLine(session.title)
        appendLine("id: ${session.id}")
        appendLine("language: ${session.languageTag}")
        appendLine("duration_ms: ${session.durationMs}")
        appendLine()
        append(session.transcript.trim())
        appendLine()
    }

    fun toMarkdown(session: ExportSession): String = buildString {
        appendLine("# ${session.title}")
        appendLine()
        appendLine("- id: `${session.id}`")
        appendLine("- language: ${session.languageTag}")
        appendLine("- created_ms: ${session.createdAtEpochMs}")
        appendLine("- duration_ms: ${session.durationMs}")
        appendLine()
        appendLine(session.transcript.trim())
        appendLine()
    }

    fun toSrt(session: ExportSession): String {
        val end = formatSrtTime(session.durationMs.coerceAtLeast(1_000L))
        return buildString {
            appendLine("1")
            appendLine("00:00:00,000 --> $end")
            appendLine(session.transcript.trim().ifEmpty { "(empty)" })
            appendLine()
        }
    }

    fun toJson(session: ExportSession): String {
        fun esc(s: String) = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
        return buildString {
            append("{")
            append("\"id\":\"${esc(session.id)}\",")
            append("\"title\":\"${esc(session.title)}\",")
            append("\"createdAtEpochMs\":${session.createdAtEpochMs},")
            append("\"durationMs\":${session.durationMs},")
            append("\"languageTag\":\"${esc(session.languageTag)}\",")
            append("\"transcript\":\"${esc(session.transcript)}\"")
            append("}")
        }
    }

    private fun formatSrtTime(ms: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(ms)
        val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val milli = ms % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, milli)
    }
}
