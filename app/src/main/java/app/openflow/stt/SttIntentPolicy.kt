package app.openflow.stt

/**
 * Phone SpeechRecognizer extras (API 33+ formatting / bias).
 * String values match [android.speech.RecognizerIntent] constants.
 */
object SttIntentPolicy {
    const val QUALITY = "quality"
    const val LATENCY = "latency"
    const val FORMATTING_API = 33

    fun preferFormatted(api: Int): Boolean = api >= FORMATTING_API

    fun formattingMode(preferQuality: Boolean): String =
        if (preferQuality) QUALITY else LATENCY

    fun includeBiasing(api: Int): Boolean = api >= FORMATTING_API

    fun languageTag(raw: String?): String = LanguagePolicy.force(raw)

    data class ListenExtras(
        val language: String,
        val putBiasing: Boolean,
        val biasing: List<String>,
    )

    fun listenExtras(
        languageTag: String?,
        api: Int,
        biasing: List<String>,
    ): ListenExtras {
        val put = includeBiasing(api)
        return ListenExtras(
            language = languageTag(languageTag),
            putBiasing = put,
            biasing = if (put) biasing else emptyList(),
        )
    }
}
