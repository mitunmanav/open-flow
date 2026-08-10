package app.openflow.search

data class SearchHit(
    val id: String,
    val title: String,
    val transcript: String,
    val createdAtEpochMs: Long
)

object TranscriptSearch {
    fun filter(items: List<SearchHit>, query: String): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return items
        val needle = q.lowercase()
        return items.filter { hit ->
            hit.title.lowercase().contains(needle) ||
                hit.transcript.lowercase().contains(needle)
        }
    }
}
