package app.openflow.text

/**
 * Strip a trailing period the way Wispr Casual/Very Casual does.
 * Never strips ?, !, or ellipsis. Formal keeps periods.
 */
object TrailingPeriodPolicy {
    const val CASUAL_SHORT_MAX = 40

    fun apply(
        text: String,
        style: WritingStyle,
        messaging: Boolean = false,
    ): String {
        val s = text.trimEnd()
        if (s.isEmpty()) return s
        if (s.endsWith("...") || s.endsWith("…")) return s
        if (!s.endsWith('.')) return s
        return when (style) {
            WritingStyle.VERY_CASUAL -> s.dropLast(1).trimEnd()
            WritingStyle.CASUAL ->
                if (messaging && s.length <= CASUAL_SHORT_MAX) s.dropLast(1).trimEnd() else s
            else -> s
        }
    }
}
