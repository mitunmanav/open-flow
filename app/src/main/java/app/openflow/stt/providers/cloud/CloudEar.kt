package app.openflow.stt.providers.cloud

import app.openflow.stt.SpeechEngine

data class EarUtterance(val text: String, val final: Boolean)

abstract class CloudEar(
    private val apiKey: () -> String,
    private val socket: CloudSocket,
    private val hasMic: () -> Boolean = { true },
) : SpeechEngine {

    private var listener: SpeechEngine.Listener? = null
    private var session: CloudSession? = null

    override val isAvailable: Boolean = true

    override fun hasMicPermission(): Boolean = hasMic()

    override fun setListener(listener: SpeechEngine.Listener?) {
        this.listener = listener
    }

    override fun startContinuous(languageTag: String) = start(languageTag)

    override fun startOnce(languageTag: String) = start(languageTag)

    override fun stop() {
        session?.close()
        session = null
        listener?.onListeningChanged(false)
    }

    override fun destroy() {
        stop()
        listener = null
    }

    protected abstract fun connectUrl(languageTag: String): String

    protected abstract fun authHeaders(key: String): Map<String, String>

    protected abstract fun parse(message: String): EarUtterance?

    private fun start(languageTag: String) {
        val key = apiKey().trim()
        if (key.isEmpty()) {
            listener?.onError("missing api key", true)
            return
        }
        if (!hasMic()) {
            listener?.onNeedMicPermission()
            return
        }
        try {
            session?.close()
            session = socket.connect(connectUrl(languageTag), authHeaders(key)) { msg ->
                val u = parse(msg) ?: return@connect
                if (u.text.isBlank()) return@connect
                if (u.final) listener?.onFinal(u.text) else listener?.onPartial(u.text)
            }
            listener?.onReady()
            listener?.onListeningChanged(true)
        } catch (e: Exception) {
            listener?.onError(e.message ?: "cloud ear failed", true)
        }
    }
}
