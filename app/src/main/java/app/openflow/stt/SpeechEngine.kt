package app.openflow.stt

import app.openflow.engine.ModelCapability

/**
 * Provider abstraction for speech-to-text.
 * MVP: [AndroidSpeechEngine] wraps [SttEngine] (default system recognizer).
 * Future: cloud or alternate engines can implement this without touching a11y/bubble.
 */
interface SpeechEngine {

    val capability: ModelCapability
        get() = ModelCapability.systemEar()

    /** Callbacks for partials, finals, errors, readiness, and listening state. */
    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String, fatal: Boolean)
        fun onReady()
        fun onListeningChanged(listening: Boolean)
        fun onNeedMicPermission() {}
        fun onRmsChanged(rmsdB: Float) {}
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

    /** API 33+ SpeechRecognizer bias. Default no-op. */
    fun setBiasing(words: List<String>) {}
}
