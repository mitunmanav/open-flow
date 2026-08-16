package app.openflow.text

/**
 * Wispr-shaped Style categories (Android Style tab).
 * Local tone only via [WritingStyle] / [StyleApplicator] — not LLM prompts.
 */
enum class StyleCategory(val label: String) {
    PERSONAL("Personal"),
    WORK("Work"),
    EMAIL("Email"),
    OTHER("Other");

    val defaultStyle: WritingStyle
        get() = when (this) {
            PERSONAL -> WritingStyle.CASUAL
            WORK, EMAIL, OTHER -> WritingStyle.FORMAL
        }

    fun allows(style: WritingStyle): Boolean = when (this) {
        PERSONAL -> style == WritingStyle.FORMAL ||
            style == WritingStyle.CASUAL ||
            style == WritingStyle.VERY_CASUAL
        WORK, EMAIL, OTHER -> style == WritingStyle.FORMAL ||
            style == WritingStyle.CASUAL ||
            style == WritingStyle.EXCITED
    }

    fun coerce(style: WritingStyle): WritingStyle =
        if (allows(style)) style else defaultStyle

    companion object {
        fun fromName(raw: String?, fallback: StyleCategory = OTHER): StyleCategory {
            val v = raw?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.name == v } ?: fallback
        }
    }
}
