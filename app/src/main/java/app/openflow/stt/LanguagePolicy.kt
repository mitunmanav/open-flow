package app.openflow.stt

/**
 * Speech language policy and global BCP-47 locale catalog.
 */
object LanguagePolicy {

    const val DEFAULT_LANGUAGE = "en-US"
    const val LOCKED = DEFAULT_LANGUAGE

    data class LanguageOption(
        val tag: String,
        val displayName: String
    )

    val SUPPORTED_LANGUAGES = listOf(
        LanguageOption("en-US", "English (United States)"),
        LanguageOption("en-GB", "English (United Kingdom)"),
        LanguageOption("en-IN", "English (India)"),
        LanguageOption("en-AU", "English (Australia)"),
        LanguageOption("en-CA", "English (Canada)"),
        LanguageOption("es-ES", "Spanish (Spain)"),
        LanguageOption("es-US", "Spanish (United States)"),
        LanguageOption("fr-FR", "French (France)"),
        LanguageOption("de-DE", "German (Germany)"),
        LanguageOption("hi-IN", "Hindi (India)"),
        LanguageOption("pt-BR", "Portuguese (Brazil)"),
        LanguageOption("it-IT", "Italian (Italy)"),
        LanguageOption("ja-JP", "Japanese (Japan)"),
        LanguageOption("zh-CN", "Chinese (Simplified)"),
    )

    private val supportedTags = SUPPORTED_LANGUAGES.map { it.tag.lowercase() }.toSet()

    fun isAllowed(tag: String?): Boolean {
        val t = tag?.trim()?.lowercase().orEmpty()
        if (t.isEmpty()) return false
        return t in supportedTags || t == "en"
    }

    fun normalize(tag: String?): String {
        val t = tag?.trim().orEmpty()
        if (t.isEmpty()) return DEFAULT_LANGUAGE
        val lower = t.lowercase()
        if (lower == "en") return "en-US"
        val found = SUPPORTED_LANGUAGES.firstOrNull { it.tag.equals(t, ignoreCase = true) }
        return found?.tag ?: DEFAULT_LANGUAGE
    }

    /** Product default when unset. Catalog langs are allowed. */
    fun force(tag: String?): String = normalize(tag)

    /** ISO 639-1 (or zh) for APIs that reject region tags. */
    fun iso639(tag: String?): String {
        val n = normalize(tag)
        return n.substringBefore('-').lowercase().ifBlank { "en" }
    }
}
