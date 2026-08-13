package app.openflow.export

import app.openflow.stt.LanguagePolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure on-device history export & search (markdown / plain text).
 * No I/O, no network — callers write/share the string.
 * Language always shown as en-US (product lock).
 */
object HistoryExport {

    private val stampFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    data class Row(
        val createdAtEpochMs: Long,
        val text: String,
        val languageTag: String = LanguagePolicy.LOCKED,
        val wordCount: Int = 0,
        val rawText: String = "",
        val id: String = "",
        val durationMs: Long = 0L,
    )

    fun toMarkdown(rows: List<Row>, includeRaw: Boolean = false): String {
        if (rows.isEmpty()) return "# Open Flow history"
        val body = rows.joinToString("\n\n") { row ->
            val stamp = stampFmt.format(Date(row.createdAtEpochMs))
            val lang = LanguagePolicy.force(row.languageTag)
            buildString {
                append("### $stamp ($lang, ${row.wordCount} words")
                if (row.durationMs > 0L) append(", ${row.durationMs}ms")
                append(")")
                if (row.id.isNotBlank()) append("\n\nid: ${row.id}")
                append("\n\n")
                append(row.text.trim())
                if (includeRaw && row.rawText.isNotBlank() && row.rawText != row.text) {
                    append("\n\n> *Raw STT:* ${row.rawText.trim()}")
                }
            }
        }
        return "# Open Flow history\n\n$body"
    }

    fun toPlainText(rows: List<Row>): String {
        if (rows.isEmpty()) return ""
        return rows.joinToString("\n\n---\n\n") { row ->
            buildString {
                val stamp = stampFmt.format(Date(row.createdAtEpochMs))
                append(stamp)
                append(" [")
                append(LanguagePolicy.force(row.languageTag))
                append("]")
                if (row.id.isNotBlank()) {
                    append(" id=")
                    append(row.id)
                }
                if (row.durationMs > 0L) {
                    append(" ")
                    append(row.durationMs)
                    append("ms")
                }
                append("\n")
                append(row.text.trim())
            }
        }
    }

    fun shareText(rows: List<Row>): String = toPlainText(rows)

    fun filterRows(rows: List<Row>, query: String): List<Row> {
        val q = query.trim()
        if (q.isEmpty()) return rows
        return rows.filter {
            it.text.contains(q, ignoreCase = true) ||
                it.rawText.contains(q, ignoreCase = true)
        }
    }
}
