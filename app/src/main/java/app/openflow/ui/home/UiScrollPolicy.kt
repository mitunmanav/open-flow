package app.openflow.ui.home

/** Stable LazyColumn keys. Duplicate day labels ("Earlier") need first-row id. */
object UiScrollPolicy {
    fun historyRowKey(id: String): String = id

    fun dayHeaderKey(label: String, firstRowId: String): String =
        "day-$label-$firstRowId"

    fun dictRowKey(id: String): String = "dict-$id"

    fun snippetRowKey(id: String): String = "snip-$id"
}
