package app.openflow.runtime

/**
 * When to drop idle STT on memory pressure.
 * [TRIM_MEMORY_BACKGROUND] is [android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND] = 40.
 */
object TrimPolicy {
    const val TRIM_MEMORY_BACKGROUND = 40

    fun shouldDropIdleStt(level: Int): Boolean = level >= TRIM_MEMORY_BACKGROUND
}
