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

    // Catalog grows toward Wispr breadth; device packs still gate real STT.
    // Tags are BCP-47 (RecognizerIntent.EXTRA_LANGUAGE).
    val SUPPORTED_LANGUAGES = listOf(
        LanguageOption("en-US", "English (United States)"),
        LanguageOption("en-GB", "English (United Kingdom)"),
        LanguageOption("en-IN", "English (India)"),
        LanguageOption("en-AU", "English (Australia)"),
        LanguageOption("en-CA", "English (Canada)"),
        LanguageOption("es-ES", "Spanish (Spain)"),
        LanguageOption("es-MX", "Spanish (Mexico)"),
        LanguageOption("es-US", "Spanish (United States)"),
        LanguageOption("fr-FR", "French (France)"),
        LanguageOption("fr-CA", "French (Canada)"),
        LanguageOption("de-DE", "German (Germany)"),
        LanguageOption("it-IT", "Italian (Italy)"),
        LanguageOption("pt-BR", "Portuguese (Brazil)"),
        LanguageOption("pt-PT", "Portuguese (Portugal)"),
        LanguageOption("nl-NL", "Dutch (Netherlands)"),
        LanguageOption("pl-PL", "Polish (Poland)"),
        LanguageOption("ru-RU", "Russian (Russia)"),
        LanguageOption("uk-UA", "Ukrainian (Ukraine)"),
        LanguageOption("tr-TR", "Turkish (Turkey)"),
        LanguageOption("ar-SA", "Arabic (Saudi Arabia)"),
        LanguageOption("he-IL", "Hebrew (Israel)"),
        LanguageOption("hi-IN", "Hindi (India)"),
        LanguageOption("bn-IN", "Bengali (India)"),
        LanguageOption("ta-IN", "Tamil (India)"),
        LanguageOption("te-IN", "Telugu (India)"),
        LanguageOption("mr-IN", "Marathi (India)"),
        LanguageOption("gu-IN", "Gujarati (India)"),
        LanguageOption("kn-IN", "Kannada (India)"),
        LanguageOption("ml-IN", "Malayalam (India)"),
        LanguageOption("pa-IN", "Punjabi (India)"),
        LanguageOption("ja-JP", "Japanese (Japan)"),
        LanguageOption("ko-KR", "Korean (Korea)"),
        LanguageOption("zh-CN", "Chinese (Simplified)"),
        LanguageOption("zh-TW", "Chinese (Traditional)"),
        LanguageOption("zh-HK", "Chinese (Hong Kong)"),
        LanguageOption("th-TH", "Thai (Thailand)"),
        LanguageOption("vi-VN", "Vietnamese (Vietnam)"),
        LanguageOption("id-ID", "Indonesian (Indonesia)"),
        LanguageOption("ms-MY", "Malay (Malaysia)"),
        LanguageOption("fil-PH", "Filipino (Philippines)"),
        LanguageOption("sv-SE", "Swedish (Sweden)"),
        LanguageOption("da-DK", "Danish (Denmark)"),
        LanguageOption("fi-FI", "Finnish (Finland)"),
        LanguageOption("nb-NO", "Norwegian (Norway)"),
        LanguageOption("cs-CZ", "Czech (Czechia)"),
        LanguageOption("ro-RO", "Romanian (Romania)"),
        LanguageOption("el-GR", "Greek (Greece)"),
        LanguageOption("hu-HU", "Hungarian (Hungary)"),
        LanguageOption("sk-SK", "Slovak (Slovakia)"),
        LanguageOption("hr-HR", "Croatian (Croatia)"),
        LanguageOption("bg-BG", "Bulgarian (Bulgaria)"),
        LanguageOption("ca-ES", "Catalan (Spain)"),
        LanguageOption("sw-KE", "Swahili (Kenya)"),
        LanguageOption("af-ZA", "Afrikaans (South Africa)"),
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
