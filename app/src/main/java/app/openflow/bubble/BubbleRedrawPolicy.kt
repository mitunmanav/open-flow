package app.openflow.bubble

/** Throttle overlay visibility + RMS label writes so a11y/RMS spam cannot thrash the bubble. */
object BubbleRedrawPolicy {
    /** Min gap between visibility refreshes triggered by a11y window/focus spam. */
    const val VISIBILITY_MIN_INTERVAL_MS = 50L

    /** Min gap between identical waveform label string writes while listening (~30fps). */
    const val RMS_LABEL_MIN_INTERVAL_MS = 33L

    fun shouldRefreshVisibility(
        lastRefreshAtMs: Long,
        nowMs: Long,
        force: Boolean = false,
    ): Boolean {
        if (force) return true
        if (lastRefreshAtMs <= 0L) return true
        return nowMs - lastRefreshAtMs >= VISIBILITY_MIN_INTERVAL_MS
    }

    fun shouldUpdateRmsLabel(
        lastBars: String,
        newBars: String,
        lastUpdateAtMs: Long,
        nowMs: Long,
    ): Boolean {
        if (newBars != lastBars) return true
        if (lastUpdateAtMs <= 0L) return true
        return nowMs - lastUpdateAtMs >= RMS_LABEL_MIN_INTERVAL_MS
    }
}
