package app.openflow.runtime

/**
 * When to drop idle STT / UI caches on memory pressure.
 *
 * Android [android.content.ComponentCallbacks2.onTrimMemory] levels we care about
 * (API 34+ only delivers these two; older constants are deprecated):
 * - [TRIM_MEMORY_UI_HIDDEN] = 20 — UI not visible. Release UI caches. Keep idle STT
 *   (bubble / a11y may still be listening).
 * - [TRIM_MEMORY_BACKGROUND] = 40 — process is in LRU, kill candidate. Drop idle STT.
 *
 * OpenFlowApp / F29: read [shouldDropIdleStt] against last onTrimMemory level.
 * Do not call bubble service from here.
 */
object TrimPolicy {
    const val TRIM_MEMORY_UI_HIDDEN = 20
    const val TRIM_MEMORY_BACKGROUND = 40

    enum class Action { KEEP, RELEASE_UI, DROP_IDLE_STT }

    fun action(level: Int): Action = when {
        level >= TRIM_MEMORY_BACKGROUND -> Action.DROP_IDLE_STT
        level >= TRIM_MEMORY_UI_HIDDEN -> Action.RELEASE_UI
        else -> Action.KEEP
    }

    fun shouldDropIdleStt(level: Int): Boolean = action(level) == Action.DROP_IDLE_STT

    fun shouldReleaseUiCaches(level: Int): Boolean = action(level) != Action.KEEP

    /** Drop idle SpeechRecognizer. Never while listen/flush. */
    fun dropIdleEngine(level: Int, listening: Boolean, stopInProgress: Boolean = false): Boolean =
        shouldDropIdleStt(level) && !listening && !stopInProgress
}
