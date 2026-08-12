package app.openflow.export

import app.openflow.stt.LanguagePolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure on-device history export (markdown / plain text).
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
    )

    fun toMarkdown(rows: List<Row>): String {
        if (rows.isEmpty()) return "# Open Flow history"
        val body = rows.joinToString("\n") { row ->
            val stamp = stampFmt.format(Date(row.createdAtEpochMs))
            val lang = LanguagePolicy.force(row.languageTag)
            "- **$stamp** ($lang, ${row.wordCount}w): ${row.text.trim()}"
        }
        return "# Open Flow history\n\n$body"
    }

    fun toPlainText(rows: List<Row>): String {
        if (rows.isEmpty()) return ""
        return rows.joinToString("\n---\n") { row ->
            buildString {
                val stamp = stampFmt.format(Date(row.createdAtEpochMs))
                append(stamp)
                append(" [")
                append(LanguagePolicy.force(row.languageTag))
                append("]\n")
                append(row.text.trim())
            }
        }
    }

    fun shareText(rows: List<Row>): String = toPlainText(rows)
}
