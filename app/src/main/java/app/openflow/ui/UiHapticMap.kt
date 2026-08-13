package app.openflow.ui

/** Extra UI haptic map. Public [android.view.HapticFeedbackConstants] ints. */
object UiHapticMap {
    const val CLOCK_TICK = 4
    const val CONTEXT_CLICK = 6
    const val CONFIRM = 16
    const val REJECT = 17
    const val KEYBOARD_TAP = 3

    enum class Event { NAV_TAB, CHIP, SLIDER, COPY, ERROR }

    fun constant(event: Event): Int = when (event) {
        Event.NAV_TAB -> CLOCK_TICK
        Event.CHIP -> CONTEXT_CLICK
        Event.SLIDER -> CLOCK_TICK
        Event.COPY -> CONFIRM
        Event.ERROR -> REJECT
    }
}
