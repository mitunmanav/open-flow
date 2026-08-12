package app.openflow.stt

/**
 * Production STT endpointer tuning.
 * Faster than stock: shorter silence → quicker finals (Wispr-feel snappiness).
 * Android docs: set silence extras only when needed; values are device-dependent.
 */
object SttTuning {
    /** Locked product language — English (US) only. No other locales. */
    const val DEFAULT_LANGUAGE = "en-US"

    /** Min listen before endpointer may fire. Lower = snappier short phrases. */
    const val MIN_SPEECH_MS = 400L

    /** Silence after speech that ends the session. */
    const val COMPLETE_SILENCE_MS = 900L

    /** Short pause tolerance mid-phrase (prevents cut on breath). */
    const val POSSIBLY_COMPLETE_SILENCE_MS = 500L

    /** Fewer alternates → slightly less work for engine. */
    const val MAX_RESULTS = 1
}
