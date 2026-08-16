package app.openflow.ui.home

/** Dict / snippet search. Wispr Android: search box + clear. */
object HubListPolicy {
    fun matches(query: String, vararg fields: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return fields.any { it.contains(q, ignoreCase = true) }
    }

    fun filterPairs(
        rows: List<Pair<String, String>>,
        query: String,
    ): List<Pair<String, String>> =
        rows.filter { matches(query, it.first, it.second) }
}
