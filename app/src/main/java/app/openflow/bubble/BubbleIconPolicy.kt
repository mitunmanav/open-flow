package app.openflow.bubble

object BubbleIconPolicy {
    fun validUri(raw: String): Boolean =
        raw.isNotBlank() && raw.startsWith("content:")
}
