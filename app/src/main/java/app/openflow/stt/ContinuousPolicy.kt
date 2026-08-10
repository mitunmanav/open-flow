package app.openflow.stt

/**
 * Pure policy for long / continuous dictation.
 * Android SpeechRecognizer ends on short silence; we restart while user still listening.
 *
 * Error codes match android.speech.SpeechRecognizer constants.
 */
data class ContinuousPolicy(
    val recreateEveryNSessions: Int = 12,
    val normalRestartDelayMs: Long = 60L,
    val busyRestartDelayMs: Long = 350L
) {
    fun shouldRestart(listening: Boolean, errorCode: Int?, hadResult: Boolean): Boolean {
        if (!listening) return false
        if (hadResult) return true
        return when (errorCode) {
            ERROR_SPEECH_TIMEOUT,
            ERROR_NO_MATCH,
            ERROR_CLIENT,
            ERROR_RECOGNIZER_BUSY,
            ERROR_NETWORK_TIMEOUT,
            ERROR_NETWORK,
            ERROR_SERVER,
            ERROR_AUDIO -> true
            ERROR_INSUFFICIENT_PERMISSIONS -> false
            null -> false
            else -> true // unknown: try again while user wants continuous
        }
    }

    fun restartDelayMs(errorCode: Int?): Long = when (errorCode) {
        ERROR_RECOGNIZER_BUSY -> busyRestartDelayMs
        else -> normalRestartDelayMs
    }

    fun shouldRecreateRecognizer(sessionCount: Int): Boolean =
        sessionCount > 0 && sessionCount % recreateEveryNSessions == 0

    companion object {
        const val ERROR_NETWORK_TIMEOUT = 1
        const val ERROR_NETWORK = 2
        const val ERROR_AUDIO = 3
        const val ERROR_SERVER = 4
        const val ERROR_CLIENT = 5
        const val ERROR_SPEECH_TIMEOUT = 6
        const val ERROR_NO_MATCH = 7
        const val ERROR_RECOGNIZER_BUSY = 8
        const val ERROR_INSUFFICIENT_PERMISSIONS = 9
    }
}
