package app.openflow.ui.home

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HistoryDays {
    data class Row(val id: String, val createdAtEpochMs: Long, val text: String)
    data class Day(val label: String, val rows: List<Row>)

    private val olderFmt = SimpleDateFormat("MMM d", Locale.US)

    fun group(rows: List<Row>, nowMs: Long, zoneOffsetMs: Long = 0L): List<Day> {
        if (rows.isEmpty()) return emptyList()
        val dayMs = 86_400_000L
        fun dayIndex(t: Long) = (t + zoneOffsetMs) / dayMs
        val today = dayIndex(nowMs)
        return rows.groupBy { dayIndex(it.createdAtEpochMs) }
            .toSortedMap(compareByDescending { it })
            .map { (idx, list) ->
                val label = when (today - idx) {
                    0L -> "Today"
                    1L -> "Yesterday"
                    else -> {
                        val dayStartMs = idx * dayMs - zoneOffsetMs
                        olderFmt.format(Date(dayStartMs))
                    }
                }
                Day(label, list)
            }
    }
}
