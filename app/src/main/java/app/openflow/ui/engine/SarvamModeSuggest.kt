package app.openflow.ui.engine

/**
 * Hinglish nudge: hi-IN / en-IN speakers usually want Sarvam's codemix
 * (Latin-script Hindi-English mix), not plain transcribe. Suggest once;
 * user confirms via the chip. Null = no suggestion.
 */
object SarvamModeSuggest {
    const val CODEMIX = "codemix"

    fun suggest(languageTag: String?, currentMode: String): String? {
        val tag = languageTag?.trim()?.lowercase().orEmpty()
        val hinglish = tag.startsWith("hi") || tag == "en-in"
        if (!hinglish) return null
        return if (currentMode == CODEMIX) null else CODEMIX
    }
}
