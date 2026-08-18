package app.openflow.ui.theme

data class AppearancePalette(
    val backgroundArgb: Int,
    val cardsArgb: Int,
    val textArgb: Int,
    val accentArgb: Int,
    val borderArgb: Int,
    val bubbleIdleArgb: Int,
    val bubbleListenArgb: Int,
    val bubbleTextArgb: Int,
) {
    companion object {
        fun factory(dark: Boolean): AppearancePalette =
            if (dark) AppearancePalette(
                backgroundArgb = 0xFF1C1B19.toInt(),
                cardsArgb = 0xFF2A2926.toInt(),
                textArgb = 0xFFF5F2EB.toInt(),
                accentArgb = 0xFFF4F1EA.toInt(),
                borderArgb = 0xFFB8B2A6.toInt(),
                bubbleIdleArgb = 0xFF1A1A18.toInt(),
                bubbleListenArgb = 0xFF1A1A18.toInt(),
                bubbleTextArgb = 0xFFF4EFE6.toInt(),
            ) else AppearancePalette(
                backgroundArgb = 0xFFF4F1EA.toInt(),
                cardsArgb = 0xFFF4F1EA.toInt(),
                textArgb = 0xFF1A1A1A.toInt(),
                accentArgb = 0xFF1A1A1A.toInt(),
                borderArgb = 0xFF1A1A1A.toInt(),
                bubbleIdleArgb = 0xFF1A1A18.toInt(),
                bubbleListenArgb = 0xFF1A1A18.toInt(),
                bubbleTextArgb = 0xFFF4EFE6.toInt(),
            )

        fun overlay(
            dark: Boolean,
            bg: String,
            cards: String,
            text: String,
            accent: String,
            border: String,
            bubbleIdle: String,
            bubbleListen: String,
            bubbleText: String,
        ): AppearancePalette {
            val f = factory(dark)
            return AppearancePalette(
                backgroundArgb = HexColor.parse(bg, f.backgroundArgb),
                cardsArgb = HexColor.parse(cards, f.cardsArgb),
                textArgb = HexColor.parse(text, f.textArgb),
                accentArgb = HexColor.parse(accent, f.accentArgb),
                borderArgb = HexColor.parse(border, f.borderArgb),
                bubbleIdleArgb = HexColor.parse(bubbleIdle, f.bubbleIdleArgb),
                bubbleListenArgb = HexColor.parse(bubbleListen, f.bubbleListenArgb),
                bubbleTextArgb = HexColor.parse(bubbleText, f.bubbleTextArgb),
            )
        }

        fun reset(): AppearancePalette = factory(false)
    }
}
