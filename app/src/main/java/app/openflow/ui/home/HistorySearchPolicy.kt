package app.openflow.ui.home

import app.openflow.data.FtsQuery

/**
 * History search: blank/noise → recent list. Else FTS MATCH for [app.openflow.data.DictationRepository.searchDictations].
 */
object HistorySearchPolicy {
    fun ftsMatch(raw: String): String? = FtsQuery.sanitize(raw)
}
