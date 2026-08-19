package app.openflow.ui

object HapticUiCopy {
    const val PAGE = "Pick a preset. Custom lets you set each bubble action."

    fun preset(feel: String): String = when (HapticFeel.normalize(feel)) {
        HapticFeel.OFF -> "No buzz."
        HapticFeel.LIGHT -> "Soft tick on every action."
        HapticFeel.FULL -> "Stronger save and cancel. Light tick when listen starts."
        HapticFeel.CUSTOM -> "Set each action below. Test after you change it."
        else -> "Stronger save and cancel. Light tick when listen starts."
    }

    fun event(event: HapticFeel.Event): Pair<String, String> = when (event) {
        HapticFeel.Event.TAP -> "Tap" to "When you tap the bubble."
        HapticFeel.Event.SAVE -> "Save" to "When Done inserts your words."
        HapticFeel.Event.CANCEL -> "Cancel" to "When you discard and do not insert."
        HapticFeel.Event.ERROR -> "Error" to "When listen fails."
        HapticFeel.Event.LISTEN -> "Listen" to "When listen starts."
    }

    fun pick(pick: String): String = when (HapticPick.normalize(pick)) {
        HapticPick.OFF -> "None"
        HapticPick.TICK -> "Soft tick"
        HapticPick.CLICK -> "Short click"
        HapticPick.CONFIRM -> "Success bump"
        HapticPick.REJECT -> "Error bump"
        else -> "Short click"
    }
}
