package app.openflow.stt

/**
 * Phone-speech on-device factory flags.
 * Default off — system SpeechRecognizer may still send audio.
 */
data class OnDeviceSpeechFlags(
    val preferOnDevice: Boolean,
    val forceOfflineOnly: Boolean,
)

object OnDeviceSpeechPolicy {
    const val DEFAULT_PREFER = false

    const val HONESTY_OFF =
        "Off: Phone speech may send audio (often Google). We have no server."

    const val HONESTY_ON =
        "On: use the phone's on-device recognizer when the OS pack exists. Missing pack = fail-soft to default (audio may leave)."

    fun flags(
        preferOnDevice: Boolean,
        offlineFallbackUsed: Boolean,
    ): OnDeviceSpeechFlags =
        OnDeviceSpeechFlags(
            preferOnDevice = preferOnDevice,
            forceOfflineOnly = preferOnDevice && !offlineFallbackUsed,
        )

    fun honesty(preferOnDevice: Boolean): String =
        if (preferOnDevice) HONESTY_ON else HONESTY_OFF
}
