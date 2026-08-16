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
}
