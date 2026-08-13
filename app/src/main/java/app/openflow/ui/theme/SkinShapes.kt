package app.openflow.ui.theme

/**
 * Shape policy. Brutal default must be hard. M3 rounded is opt-in only.
 * Cards use [Slot.MEDIUM] via MaterialTheme.shapes.medium.
 */
object SkinShapes {
    enum class Slot { EXTRA_SMALL, SMALL, MEDIUM, LARGE, EXTRA_LARGE }

    fun cornerDp(skin: VisualSkin, slot: Slot = Slot.MEDIUM): Int = when (skin) {
        VisualSkin.BRUTAL -> 0
        VisualSkin.M3 -> when (slot) {
            Slot.EXTRA_SMALL -> 8
            Slot.SMALL -> 12
            Slot.MEDIUM -> 16
            Slot.LARGE -> 20
            Slot.EXTRA_LARGE -> 28
        }
    }

    fun isHard(skin: VisualSkin): Boolean = cornerDp(skin) == 0
}
