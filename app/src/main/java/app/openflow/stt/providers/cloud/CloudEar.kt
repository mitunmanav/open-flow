package app.openflow.stt.providers.cloud

import app.openflow.stt.SpeechEngine

data class EarUtterance(val text: String, val final: Boolean)

abstract class CloudEar(
    private val apiKey: () -> String,
    private val socket: CloudSocket,
    private val hasMic: () -> Boolean = { true },
    private val pcm: PcmSource = PcmSource.None,
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
        pcm.stop()
        val live = session
        if (live != null) {
            try {
                onSessionClose(live)
            } catch (_: Exception) {
            }
            live.close()
        }
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
            pcm.stop()
            session?.close()
            val live = socket.connect(
                url = connectUrl(languageTag),
                headers = authHeaders(key),
                onError = { err ->
                    pcm.stop()
                    session = null
                    listener?.onListeningChanged(false)
                    listener?.onError(err, true)
                },
                onText = { msg ->
                    val u = parse(msg) ?: return@connect
                    if (u.text.isBlank()) return@connect
                    if (u.final) listener?.onFinal(u.text) else listener?.onPartial(u.text)
                },
            )
            session = live
            onSessionOpen(live)
            pcm.start { chunk ->
                if (chunk.isNotEmpty()) writeAudio(live, chunk)
            }
            listener?.onReady()
            listener?.onListeningChanged(true)
        } catch (e: Exception) {
            pcm.stop()
            session = null
            listener?.onError(e.message ?: "cloud ear failed", true)
        }
    }

    /** Default: raw PCM binary (Deepgram / AssemblyAI). */
    protected open fun writeAudio(session: CloudSession, pcm: ByteArray) {
        session.send(pcm)
    }

    /** After connect, before mic. OpenAI session.update, etc. */
    protected open fun onSessionOpen(session: CloudSession) = Unit

    /** Before socket close. Terminate / CloseStream / flush / commit. */
    protected open fun onSessionClose(session: CloudSession) = Unit
}
