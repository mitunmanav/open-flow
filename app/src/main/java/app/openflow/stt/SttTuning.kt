package app.openflow.stt

/**
 * STT endpointer tuning.
 * Slightly more forgiving than ultra-snappy values (fewer false timeouts).
 * Android docs: silence extras are device-dependent; some engines ignore them.
 */
object SttTuning {
    /** Prefer English (US) for quality focus; user can override in prefs. */
    const val DEFAULT_LANGUAGE = "en-US"

    /** Min listen before endpointer may fire. */
    const val MIN_SPEECH_MS = 600L

    /** Silence after speech that ends the utterance. */
    const val COMPLETE_SILENCE_MS = 1200L

    /** Short pause tolerance mid-phrase. */
    const val POSSIBLY_COMPLETE_SILENCE_MS = 700L

    const val MAX_RESULTS = 3
}
