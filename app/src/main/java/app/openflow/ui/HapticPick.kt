package app.openflow.ui

import androidx.compose.runtime.compositionLocalOf

object HapticPick {
    const val OFF = "off"
    const val TICK = "tick"
    const val CLICK = "click"
    const val CONFIRM = "confirm"
    const val REJECT = "reject"

    fun normalize(value: String): String = when (value.lowercase()) {
        OFF, TICK, CLICK, CONFIRM, REJECT -> value.lowercase()
        else -> CLICK
    }

    fun constant(pick: String): Int? = when (normalize(pick)) {
        OFF -> null
        TICK -> HapticFeel.CLOCK_TICK
        CLICK -> HapticFeel.CONTEXT_CLICK
        CONFIRM -> HapticFeel.CONFIRM
        REJECT -> HapticFeel.REJECT
        else -> HapticFeel.CONTEXT_CLICK
    }
}

val LocalHapticTap = compositionLocalOf { HapticPick.CLICK }
