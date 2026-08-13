package app.openflow.stt

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Default [SpeechEngine]: thin wrapper around existing [SttEngine].
 * Keeps FlowAccessibilityService on SttEngine until callers switch to this type.
 */
class AndroidSpeechEngine(
    context: Context,
    preferOnDevice: Boolean = true,
    policy: ContinuousPolicy = ContinuousPolicy(),
    mainHandler: Handler = Handler(Looper.getMainLooper()),
    softMuteBeeps: Boolean = false,
    /** Optional inject for tests; default builds a real [SttEngine]. */
    private val engine: SttEngine = SttEngine(
        context = context,
        preferOnDevice = preferOnDevice,
        policy = policy,
        mainHandler = mainHandler,
        softMuteBeeps = softMuteBeeps
    )
) : SpeechEngine {

    private var outer: SpeechEngine.Listener? = null

    private val bridge = object : SttEngine.Listener {
        override fun onPartial(text: String) {
            outer?.onPartial(text)
        }

        override fun onFinal(text: String) {
            outer?.onFinal(text)
        }

        override fun onError(message: String, fatal: Boolean) {
            outer?.onError(message, fatal)
        }

        override fun onReady() {
            outer?.onReady()
        }

        override fun onListeningChanged(listening: Boolean) {
            outer?.onListeningChanged(listening)
        }

        override fun onNeedMicPermission() {
            outer?.onNeedMicPermission()
        }

        override fun onRmsChanged(rmsdB: Float) {
            outer?.onRmsChanged(rmsdB)
        }
    }

    init {
        engine.setListener(bridge)
    }

    override val isAvailable: Boolean
        get() = engine.isAvailable

    override fun hasMicPermission(): Boolean = engine.hasMicPermission()

    override fun setListener(listener: SpeechEngine.Listener?) {
        outer = listener
        // Keep bridge installed so late setListener still receives events.
        engine.setListener(bridge)
    }

    override fun setBiasing(words: List<String>) {
        engine.setBiasing(words)
    }

    override fun startContinuous(languageTag: String) {
        engine.startContinuous(languageTag)
    }

    override fun startOnce(languageTag: String) {
        engine.startOnce(languageTag)
    }

    override fun stop() {
        engine.stop()
    }

    /** Drain last final. Not on [SpeechEngine] — other impls stay stop-only. */
    fun stopAndFlush(timeoutMs: Long, onDone: () -> Unit) {
        engine.stopAndFlush(timeoutMs, onDone)
    }

    override fun destroy() {
        outer = null
        engine.destroy()
    }
}
