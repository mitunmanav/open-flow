package app.openflow.stt

/**
 * Product lock: English (en-US) only.
 * Any other BCP-47 tag is rejected and forced to [SttTuning.DEFAULT_LANGUAGE].
 */
object LanguagePolicy {

    const val LOCKED = SttTuning.DEFAULT_LANGUAGE // en-US

    /** True only for bare `en` or `en-US` (case-insensitive). */
    fun isAllowed(tag: String?): Boolean {
        val t = tag?.trim().orEmpty()
        if (t.isEmpty()) return false
        val lower = t.lowercase()
        return lower == "en" || lower == "en-us"
    }

    /** Always returns en-US. Non-English and other en-* variants → en-US. */
    fun force(tag: String?): String = LOCKED
}
