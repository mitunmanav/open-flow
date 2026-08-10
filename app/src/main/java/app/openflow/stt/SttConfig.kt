package app.openflow.stt

/** Pure config for STT — testable without device. */
data class SttConfig(
    val preferOnDevice: Boolean = true,
    val partialResults: Boolean = true,
    val maxResults: Int = 3
) {
    fun extras(): Map<String, Any> = buildMap {
        put(KEY_PREFER_OFFLINE, preferOnDevice)
        put(KEY_PARTIAL, partialResults)
        put(KEY_MAX_RESULTS, maxResults)
    }

    companion object {
        const val KEY_PREFER_OFFLINE = "android.speech.extra.PREFER_OFFLINE"
        const val KEY_PARTIAL = "android.speech.extra.PARTIAL_RESULTS"
        const val KEY_MAX_RESULTS = "android.speech.extra.MAX_RESULTS"
    }
}
