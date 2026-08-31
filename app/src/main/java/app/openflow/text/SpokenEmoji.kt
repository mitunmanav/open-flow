package app.openflow.text

/**
 * Opt-in spoken → emoji ("smile emoji" → 😄). Wispr ships this; we keep it off by default.
 * Whole-phrase, case-insensitive. No invention beyond the map.
 */
object SpokenEmoji {
    /** Longest phrases first so "heart eyes emoji" wins over "heart emoji". */
    private val PHRASES: List<Pair<String, String>> = listOf(
        "heart eyes emoji" to "😍",
        "thumbs down emoji" to "👎",
        "thumbs up emoji" to "👍",
        "rolling eyes emoji" to "🙄",
        "thinking emoji" to "🤔",
        "fire emoji" to "🔥",
        "clap emoji" to "👏",
        "pray emoji" to "🙏",
        "wave emoji" to "👋",
        "ok emoji" to "👌",
        "cry emoji" to "😢",
        "sad emoji" to "😢",
        "angry emoji" to "😠",
        "laugh emoji" to "😂",
        "lol emoji" to "😂",
        "smile emoji" to "😄",
        "smiling emoji" to "😄",
        "wink emoji" to "😉",
        "heart emoji" to "❤️",
        "love emoji" to "❤️",
        "check emoji" to "✅",
        "cross emoji" to "❌",
        "star emoji" to "⭐",
        "party emoji" to "🎉",
        "eyes emoji" to "👀",
    )

    fun apply(text: String, enabled: Boolean): String {
        if (!enabled || text.isBlank()) return text
        var t = text
        for ((phrase, emoji) in PHRASES) {
            t = Regex("(?i)\\b${Regex.escape(phrase)}\\b").replace(t, emoji)
        }
        return t
    }
}
