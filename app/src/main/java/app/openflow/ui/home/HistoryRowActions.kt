package app.openflow.ui.home

object HistoryRowActions {
    fun primary(): List<String> = listOf("Copy", "Share")

    fun more(hasRaw: Boolean): List<String> =
        buildList {
            add("Edit")
            if (hasRaw) {
                add("Show raw")
                add("Use raw")
            }
            add("Delete")
        }
}
