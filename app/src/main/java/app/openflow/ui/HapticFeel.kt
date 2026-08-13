package app.openflow.ui

/**
 * Bubble haptic strength. Pure ints — JVM tests need no Android.
 *
 * CLOCK_TICK = 4, CONTEXT_CLICK = 6, CONFIRM = 16, REJECT = 17.
 */
object HapticFeel {
    const val OFF = "off"
    const val LIGHT = "light"
    const val FULL = "full"

    const val CLOCK_TICK = 4
    const val CONTEXT_CLICK = 6
    const val CONFIRM = 16
    const val REJECT = 17

    enum class Event { TAP, SAVE, CANCEL }

    fun normalize(value: String): String = when (value.lowercase()) {
        OFF, LIGHT, FULL -> value.lowercase()
        else -> FULL
    }

    /** null = skip haptic (off). Light = CLOCK_TICK only. */
    fun constantFor(feel: String, event: Event): Int? = when (normalize(feel)) {
        OFF -> null
        LIGHT -> CLOCK_TICK
        else -> when (event) {
            Event.TAP -> CONTEXT_CLICK
            Event.SAVE -> CONFIRM
            Event.CANCEL -> REJECT
        }
    }
}
