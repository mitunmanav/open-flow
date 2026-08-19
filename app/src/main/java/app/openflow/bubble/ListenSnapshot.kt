package app.openflow.bubble

data class ListenSnapshot(
    val generation: Int,
    val finals: String,
    val lastPartial: String,
    val prefix: String,
    val earId: String,
    val sessionId: String,
    val startedAtElapsed: Long,
    val startedWallMs: Long,
    val retrySessionId: String?,
) {
    val raw: String get() = SessionText.commitRaw(finals, lastPartial)
    val isRetry: Boolean get() = retrySessionId != null
    fun durationMs(nowElapsed: Long): Long = (nowElapsed - startedAtElapsed).coerceAtLeast(0L)
}
