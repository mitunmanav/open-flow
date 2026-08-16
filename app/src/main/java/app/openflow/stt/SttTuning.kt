package app.openflow.stt

/**
 * STT endpointer + formatting knobs.
 *
 * Silence extras are device-dependent; some engines ignore them.
 * Formatting extras require API 33+ (applied in [SttEngine] with version guards).
 *
 * Tradeoff (API 33+ [android.speech.RecognizerIntent.EXTRA_ENABLE_FORMATTING]):
 * - **Quality** ([preferFormattingQuality]=true): better auto punctuation; higher latency.
 * - **Latency** (false): snappier partials/finals; local cleanup pipeline still polishes text.
 *
 * Profiles (prefs `stt_profile`):
 * - **fast** — shorter silence, latency formatting (beat cloud-feel wait)
 * - **balanced** — default (ship)
 * - **accurate** — longer silence, quality formatting
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

        // Balanced defaults — snappier than old 1200/700 without cutting mid-phrase hard.
        const val DEFAULT_MIN_SPEECH_MS = 400L
        const val DEFAULT_COMPLETE_SILENCE_MS = 850L
        const val DEFAULT_POSSIBLY_COMPLETE_SILENCE_MS = 480L
        const val DEFAULT_MAX_RESULTS = 3

        /** Default quality formatting (API 33+): better punctuation than latency mode. */
        const val DEFAULT_PREFER_FORMATTING_QUALITY = true

        const val PROFILE_FAST = "fast"
        const val PROFILE_BALANCED = "balanced"
        const val PROFILE_ACCURATE = "accurate"

        fun normalizeProfile(value: String): String = when (value.lowercase()) {
            PROFILE_FAST, PROFILE_ACCURATE -> value.lowercase()
            else -> PROFILE_BALANCED
        }

        fun forProfile(profile: String): SttTuning = when (normalizeProfile(profile)) {
            PROFILE_FAST -> SttTuning(
                minSpeechMs = 280L,
                completeSilenceMs = 550L,
                possiblyCompleteSilenceMs = 320L,
                maxResults = 2,
                preferFormattingQuality = false
            )
            PROFILE_ACCURATE -> SttTuning(
                minSpeechMs = 600L,
                completeSilenceMs = 1400L,
                possiblyCompleteSilenceMs = 800L,
                maxResults = 5,
                preferFormattingQuality = true
            )
            else -> SttTuning() // balanced field defaults
        }

        /** Historical aliases (same as field defaults). */
        const val MIN_SPEECH_MS = DEFAULT_MIN_SPEECH_MS
        const val COMPLETE_SILENCE_MS = DEFAULT_COMPLETE_SILENCE_MS
        const val POSSIBLY_COMPLETE_SILENCE_MS = DEFAULT_POSSIBLY_COMPLETE_SILENCE_MS
        const val MAX_RESULTS = DEFAULT_MAX_RESULTS
    }
}
