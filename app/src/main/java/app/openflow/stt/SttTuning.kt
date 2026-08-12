package app.openflow.stt

/**
 * STT endpointer + formatting knobs.
 *
 * Silence extras are device-dependent; some engines ignore them.
 * Formatting extras require API 33+ (applied in [SttEngine] with version guards).
 *
 * Tradeoff (API 33+ [android.speech.RecognizerIntent.EXTRA_ENABLE_FORMATTING]):
 * - **Quality** ([preferFormattingQuality]=true, default): better auto punctuation /
 *   capitalization; higher latency.
 * - **Latency** (false): snappier partials/finals; weaker auto punct.
 */
data class SttTuning(
    /** Min listen before endpointer may fire. */
    val minSpeechMs: Long = DEFAULT_MIN_SPEECH_MS,
    /** Silence after speech that ends the utterance. */
    val completeSilenceMs: Long = DEFAULT_COMPLETE_SILENCE_MS,
    /** Short pause tolerance mid-phrase. */
    val possiblyCompleteSilenceMs: Long = DEFAULT_POSSIBLY_COMPLETE_SILENCE_MS,
    val maxResults: Int = DEFAULT_MAX_RESULTS,
    /**
     * API 33+: prefer [RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY] when true,
     * else [RecognizerIntent.FORMATTING_OPTIMIZE_LATENCY].
     */
    val preferFormattingQuality: Boolean = DEFAULT_PREFER_FORMATTING_QUALITY,
) {
    companion object {
        /** Locked product language — English (US) only. No other locales. */
        const val DEFAULT_LANGUAGE = "en-US"

        const val DEFAULT_MIN_SPEECH_MS = 600L
        const val DEFAULT_COMPLETE_SILENCE_MS = 1200L
        const val DEFAULT_POSSIBLY_COMPLETE_SILENCE_MS = 700L
        const val DEFAULT_MAX_RESULTS = 3

        /** Default quality for dictation readability over partial speed. */
        const val DEFAULT_PREFER_FORMATTING_QUALITY = true

        /** Historical aliases (same as field defaults). */
        const val MIN_SPEECH_MS = DEFAULT_MIN_SPEECH_MS
        const val COMPLETE_SILENCE_MS = DEFAULT_COMPLETE_SILENCE_MS
        const val POSSIBLY_COMPLETE_SILENCE_MS = DEFAULT_POSSIBLY_COMPLETE_SILENCE_MS
        const val MAX_RESULTS = DEFAULT_MAX_RESULTS
    }
}
