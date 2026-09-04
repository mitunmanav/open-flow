package app.openflow.core.common

/**
 * When to drop idle STT / UI caches on memory pressure.
 * Pure Kotlin. No Android imports.
 * Levels mirror ComponentCallbacks2: UI_HIDDEN = 20, BACKGROUND = 40.
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
