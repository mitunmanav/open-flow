package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HistoryDaysTest {
    @Test
    fun groups_today_and_yesterday() {
        val today = 1_800_000_000_000L
        val yesterday = today - 86_400_000L
        val rows = listOf(
            HistoryDays.Row("a", today, "hi"),
            HistoryDays.Row("b", yesterday, "old")
        )
        val g = HistoryDays.group(rows, nowMs = today, zoneOffsetMs = 0L)
        assertThat(g.map { it.label }).containsExactly("Today", "Yesterday").inOrder()
        assertThat(g[0].rows.map { it.id }).containsExactly("a")
        assertThat(g[1].rows.map { it.id }).containsExactly("b")
    }

    @Test
    fun empty_input_empty_list() {
        assertThat(HistoryDays.group(emptyList(), nowMs = 1_800_000_000_000L)).isEmpty()
    }

    @Test
    fun older_days_get_distinct_date_labels() {
        val today = 1_800_000_000_000L
        val older1 = today - 3 * 86_400_000L
        val older2 = today - 5 * 86_400_000L
        val rows = listOf(
            HistoryDays.Row("a", older1, "three"),
            HistoryDays.Row("b", older2, "five")
        )
        val g = HistoryDays.group(rows, nowMs = today, zoneOffsetMs = 0L)
        assertThat(g).hasSize(2)
        assertThat(g[0].label).isNotEqualTo("Earlier")
        assertThat(g[1].label).isNotEqualTo("Earlier")
        assertThat(g[0].label).isNotEqualTo(g[1].label)
        assertThat(g[0].label).matches("[A-Z][a-z]{2} \\d{1,2}")
        assertThat(g[1].label).matches("[A-Z][a-z]{2} \\d{1,2}")
    }

    @Test
    fun same_calendar_day_one_section() {
        val today = 1_800_000_000_000L
        val rows = listOf(
            HistoryDays.Row("a", today, "hi"),
            HistoryDays.Row("b", today + 3_600_000L, "later")
        )
        val g = HistoryDays.group(rows, nowMs = today, zoneOffsetMs = 0L)
        assertThat(g).hasSize(1)
        assertThat(g[0].label).isEqualTo("Today")
        assertThat(g[0].rows.map { it.id }).containsExactly("a", "b")
    }
}
