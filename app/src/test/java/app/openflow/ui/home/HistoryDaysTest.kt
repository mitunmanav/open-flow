package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HistoryDaysTest {
    @Test
    fun groups_today_and_older() {
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
}
