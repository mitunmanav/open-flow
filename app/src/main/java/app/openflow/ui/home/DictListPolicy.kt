package app.openflow.ui.home

import java.util.Locale

/** Wispr desktop dictionary sort: newest, oldest, A–Z. */
object DictListPolicy {
    enum class Sort { NEWEST, OLDEST, ALPHA }

    fun sort(rows: List<Pair<Long, String>>, sort: Sort): List<Pair<Long, String>> =
        apply(rows, sort, createdAt = { it.first }, label = { it.second })

    fun <T> apply(
        items: List<T>,
        sort: Sort,
        createdAt: (T) -> Long,
        label: (T) -> String,
    ): List<T> = when (sort) {
        Sort.NEWEST -> items.sortedByDescending(createdAt)
        Sort.OLDEST -> items.sortedBy(createdAt)
        Sort.ALPHA -> items.sortedWith(compareBy { label(it).lowercase(Locale.ROOT) })
    }

    fun fromPref(raw: String): Sort = when (raw.uppercase(Locale.ROOT)) {
        Sort.NEWEST.name -> Sort.NEWEST
        Sort.OLDEST.name -> Sort.OLDEST
        else -> Sort.ALPHA
    }
}
