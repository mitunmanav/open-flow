package app.openflow.ui.home

object HistoryDays {
    data class Row(val id: String, val createdAtEpochMs: Long, val text: String)
    data class Day(val label: String, val rows: List<Row>)

    fun group(rows: List<Row>, nowMs: Long, zoneOffsetMs: Long = 0L): List<Day> {
        val dayMs = 86_400_000L
        fun dayIndex(t: Long) = (t + zoneOffsetMs) / dayMs
        val today = dayIndex(nowMs)
        return rows.groupBy { dayIndex(it.createdAtEpochMs) }
            .toSortedMap(compareByDescending { it })
            .map { (idx, list) ->
                val label = when (today - idx) {
                    0L -> "Today"
                    1L -> "Yesterday"
                    else -> "Earlier"
                }
                Day(label, list)
            }
    }
}
