package app.openflow.ui

/**
 * Maps dictation events to [android.view.HapticFeedbackConstants] ints.
 * Pure ints so JVM unit tests need no Android.
 *
 * CONFIRM = 16, REJECT = 17 (API 30), CLOCK_TICK = 4.
 */
object Haptics {
    /** HapticFeedbackConstants.CONFIRM */
    const val CONFIRM = 16

    /** HapticFeedbackConstants.REJECT */
    const val REJECT = 17

    /** HapticFeedbackConstants.CLOCK_TICK */
    const val CLOCK_TICK = 4

    enum class Event { INSERT, CANCEL, TICK }

    fun constantFor(event: Event): Int = when (event) {
        Event.INSERT -> CONFIRM
        Event.CANCEL -> REJECT
        Event.TICK -> CLOCK_TICK
    }
}
