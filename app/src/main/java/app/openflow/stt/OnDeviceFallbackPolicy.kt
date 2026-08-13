package app.openflow.stt

/**
 * Pure rules: prefer on-device SpeechRecognizer, fail-soft to default (no crash).
 * Matches Android SpeechRecognizer error ints for LANGUAGE_* (API 31+).
 */
data class OnDeviceFallbackPolicy(
    val softFallbackDelayMs: Long = 350L,
) {
    fun tryOnDeviceFactory(
        preferOnDevice: Boolean,
        forceOfflineOnly: Boolean,
        offlineFallbackUsed: Boolean,
        onDeviceAvailable: Boolean,
    ): Boolean =
        preferOnDevice &&
            forceOfflineOnly &&
            !offlineFallbackUsed &&
            onDeviceAvailable

    /** Factory throw / missing pack — never fatal. */
    fun factoryFailureIsFatal(): Boolean = false

    fun canSoftFallback(alreadyUsed: Boolean): Boolean = !alreadyUsed

    fun shouldSoftFallback(errorCode: Int): Boolean = when (errorCode) {
        ContinuousPolicy.ERROR_CLIENT,
        ContinuousPolicy.ERROR_SERVER,
        ContinuousPolicy.ERROR_NETWORK,
        ContinuousPolicy.ERROR_NETWORK_TIMEOUT,
        ContinuousPolicy.ERROR_LANGUAGE_NOT_SUPPORTED,
        ContinuousPolicy.ERROR_LANGUAGE_UNAVAILABLE -> true
        else -> false
    }
}
