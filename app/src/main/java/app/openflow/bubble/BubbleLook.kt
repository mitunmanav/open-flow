package app.openflow.bubble

import app.openflow.ui.theme.BubbleTint
import app.openflow.ui.theme.HexColor

/** Overlay fill/on-text: custom hex wins; empty/bad hex uses Bubble tint preset. */
object BubbleLook {
    fun fillArgb(hex: String, tint: String): Int =
        HexColor.parse(hex, BubbleTint.argb(tint))

    fun onArgb(hex: String, tint: String): Int =
        HexColor.parse(hex, BubbleTint.onArgb(tint))
}
