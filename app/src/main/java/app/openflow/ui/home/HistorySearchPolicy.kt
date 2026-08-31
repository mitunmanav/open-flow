package app.openflow.ui.home

import app.openflow.data.FtsQuery

/**
 * History search: blank/noise → recent list. Else FTS MATCH for [app.openflow.data.DictationRepository.searchDictations].
 * [mergeSearch] unions the loaded page with full-history FTS hits (HomeFeed).
 */
object HistorySearchPolicy {
    fun ftsMatch(raw: String): String? = FtsQuery.sanitize(raw)

    fun <T> mergeSearch(
        query: String,
        loadedPage: List<T>,
        fullHits: List<T>,
        id: (T) -> String,
        createdAt: (T) -> Long,
    ): List<T> {
        val local = loadedPage
        if (query.isBlank()) return local
        val seen = local.mapTo(HashSet()) { id(it) }
        return (local + fullHits.filter { id(it) !in seen })
            .sortedByDescending(createdAt)
    }
}
