package app.openflow.ui.home

object HomeStatsPager {
    data class Page(val value: String, val label: String)

    fun pages(words: Long, sessions: Long, streak: Int): List<Page> = listOf(
        Page(HomeStatsCopy.words(words), "Words"),
        Page(HomeStatsCopy.sessions(sessions), "Sessions"),
        Page(HomeStatsCopy.streak(streak), "Streak"),
    )

    fun clamp(index: Int, count: Int): Int =
        if (count <= 0) 0 else index.coerceIn(0, count - 1)
}
