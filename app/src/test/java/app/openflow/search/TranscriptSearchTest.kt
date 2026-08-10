package app.openflow.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TranscriptSearchTest {

    private val sessions = listOf(
        SearchHit("1", "Standup", "Ship the budget feature today", 100L),
        SearchHit("2", "Call", "Talked about marketing budget", 200L),
        SearchHit("3", "Note", "Lunch plans only", 300L)
    )

    @Test
    fun empty_query_returns_all() {
        val result = TranscriptSearch.filter(sessions, "")
        assertThat(result).hasSize(3)
    }

    @Test
    fun keyword_matches_title_or_body() {
        val result = TranscriptSearch.filter(sessions, "budget")
        assertThat(result.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun case_insensitive() {
        val result = TranscriptSearch.filter(sessions, "LUNCH")
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("3")
    }

    @Test
    fun no_match() {
        val result = TranscriptSearch.filter(sessions, "quantum")
        assertThat(result).isEmpty()
    }
}
