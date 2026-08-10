package app.openflow.stt

/**
 * Provider abstraction for speech-to-text.
 * MVP: [AndroidSpeechEngine] wraps [SttEngine] (on-device prefer).
 * Future: cloud or alternate engines can implement this without touching a11y/bubble.
 */
interface SpeechEngine {

    /** Callbacks for partials, finals, errors, readiness, and listening state. */
    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String, fatal: Boolean)
        fun onReady()
        fun onListeningChanged(listening: Boolean)
        fun onNeedMicPermission() {}
    }

    /** True when a speech recognizer is available on this device. */
    val isAvailable: Boolean

    fun hasMicPermission(): Boolean

    fun setListener(listener: Listener?)

    /** Continuous dictation with auto-restart while listening. */
    fun startContinuous(languageTag: String)

    /** Single utterance; stops after one final (or fatal error). */
    fun startOnce(languageTag: String)

    fun stop()

    fun destroy()
}
