package app.openflow.stt.providers.host

import app.openflow.ai.providers.host.HostUrl
import app.openflow.stt.SpeechEngine

/**
 * Laptop / LAN ear stub. Same OpenAI audio shape later.
 * Missing or public-http URL → [SpeechEngine.Listener.onError]. No crash.
 */
class LaptopEar(
    private val baseUrl: String?,
    private val micGranted: Boolean = true,
) : SpeechEngine {
    val streamLive: Boolean = true
    val audioLeavesDevice: Boolean = true
    val needsNet: Boolean = true

    private var listener: SpeechEngine.Listener? = null

    override val isAvailable: Boolean
        get() = HostUrl.allow(baseUrl)

    override fun hasMicPermission(): Boolean = micGranted

    override fun setListener(listener: SpeechEngine.Listener?) {
        this.listener = listener
    }

    override fun startContinuous(languageTag: String) {
        start(languageTag)
    }

    override fun startOnce(languageTag: String) {
        start(languageTag)
    }

    override fun stop() {
        listener?.onListeningChanged(false)
    }

    override fun destroy() {
        listener = null
    }

    private fun start(languageTag: String) {
        if (!HostUrl.allow(baseUrl)) {
            listener?.onError("missing laptop url", fatal = true)
            return
        }
        listener?.onReady()
        listener?.onListeningChanged(true)
    }
}
