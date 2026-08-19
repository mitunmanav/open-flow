package app.openflow.export

import app.openflow.orchestrate.SharePayload
import app.openflow.stt.LanguagePolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure on-device history export & search (markdown / plain text).
 * No I/O, no network — callers write/share the string.
 * Language shown from catalog via [LanguagePolicy.normalize].
 */
object HistoryExport {

    private val stampFmt: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

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
            val lang = LanguagePolicy.normalize(row.languageTag)
            buildString {
                append("### $stamp ($lang, ${row.wordCount} words")
                if (row.durationMs > 0L) append(", ${row.durationMs}ms")
                append(")")
                if (row.id.isNotBlank()) append("\n\nid: ${row.id}")
                append("\n\n")
                val body = SharePayload.forRow(row.text, row.rawText)
                append(body)
                if (includeRaw && row.rawText.isNotBlank() &&
                    row.text.trim().isNotEmpty() && row.rawText.trim() != row.text.trim()
                ) {
                    append("\n\n> *Raw STT:* ${row.rawText.trim()}")
                }
            }
        }
        return "# Open Flow history\n\n$body"
    }

    fun shareText(rows: List<Row>): String = toPlainText(rows)

    fun render(rows: List<Row>, choice: ExportChoice): String =
        when (choice.format) {
            ExportFormat.MARKDOWN -> toMarkdown(rows, choice.includeRaw)
            ExportFormat.PLAIN -> toPlainText(rows, choice.includeRaw)
            ExportFormat.JSON -> toJson(rows, choice.includeRaw)
        }

    fun toPlainText(rows: List<Row>, includeRaw: Boolean = false): String {
        if (rows.isEmpty()) return ""
        return rows.joinToString("\n\n---\n\n") { row ->
            buildString {
                val stamp = stampFmt.format(Date(row.createdAtEpochMs))
                append(stamp)
                append(" [")
                append(LanguagePolicy.normalize(row.languageTag))
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
                append(SharePayload.forRow(row.text, row.rawText))
                if (includeRaw && row.rawText.isNotBlank() &&
                    row.rawText.trim() != row.text.trim()
                ) {
                    append("\nRAW: ")
                    append(row.rawText.trim())
                }
            }
        }
    }

    fun toJson(rows: List<Row>, includeRaw: Boolean): String {
        if (rows.isEmpty()) return "[]"
        return rows.joinToString(prefix = "[", postfix = "]") { row ->
            buildString {
                append("{")
                append("\"id\":").append(jsonStr(row.id)).append(',')
                append("\"createdAtEpochMs\":").append(row.createdAtEpochMs).append(',')
                append("\"languageTag\":").append(jsonStr(LanguagePolicy.normalize(row.languageTag))).append(',')
                append("\"text\":").append(jsonStr(SharePayload.forRow(row.text, row.rawText))).append(',')
                append("\"wordCount\":").append(row.wordCount).append(',')
                append("\"durationMs\":").append(row.durationMs)
                if (includeRaw) {
                    append(",\"rawText\":").append(jsonStr(row.rawText))
                }
                append("}")
            }
        }
    }

    private fun jsonStr(s: String): String {
        val b = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> b.append("\\\\")
                '"' -> b.append("\\\"")
                '\n' -> b.append("\\n")
                '\r' -> b.append("\\r")
                '\t' -> b.append("\\t")
                else -> b.append(c)
            }
        }
        return b.append('"').toString()
    }

    fun filterRows(rows: List<Row>, query: String): List<Row> {
        val q = query.trim()
        if (q.isEmpty()) return rows
        return rows.filter {
            it.text.contains(q, ignoreCase = true) ||
                it.rawText.contains(q, ignoreCase = true)
        }
    }
}
