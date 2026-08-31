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
    // stopAndFlush pending state — races handled via atomic done guard.
    @Volatile private var pendingFinish: (() -> Unit)? = null
    @Volatile private var pendingHandler: android.os.Handler? = null
    @Volatile private var pendingTimeout: Runnable? = null
    private var pendingDone: java.util.concurrent.atomic.AtomicBoolean? = null
    // M3-A tee: when >=0, PCM is fed externally via feedTeePcm and internal PcmSource is not used.
    @Volatile private var teeGeneration: Int = -1

    override val isAvailable: Boolean = true

    override fun hasMicPermission(): Boolean = hasMic()

    override fun setListener(listener: SpeechEngine.Listener?) {
        this.listener = listener
    }

    override fun startContinuous(languageTag: String) = start(languageTag)

    override fun startOnce(languageTag: String) = start(languageTag)

    /** M3-A: set active tee generation before start. -1 means use internal PcmSource. */
    fun setTeeGeneration(gen: Int) { teeGeneration = gen }

    /** M3-A: feed PCM from AppAudioCapture; drops stale generation. */
    fun feedTeePcm(pcmBytes: ByteArray, generation: Int) {
        if (generation != teeGeneration) return
        if (pcmBytes.isEmpty()) return
        val live = session ?: return
        writeAudio(live, pcmBytes)
    }

    override fun stop() {
        val wasTee = teeGeneration != -1
        teeGeneration = -1
        if (!wasTee) pcm.stop()
        val live = session
        session = null
        // Cancel any pending flush — stop is immediate, no wait.
        pendingTimeout?.let { pendingHandler?.removeCallbacks(it) }
        pendingFinish = null
        pendingHandler = null
        pendingTimeout = null
        pendingDone?.set(true)
        pendingDone = null
        if (live != null) {
            runCatching { onSessionClose(live) }
            live.close()
        }
        listener?.onListeningChanged(false)
    }

    override fun stopAndFlush(timeoutMs: Long, onDone: () -> Unit) {
        val live = session
        session = null
        val wasTee = teeGeneration != -1
        teeGeneration = -1
        if (!wasTee) pcm.stop()
        if (live == null) {
            listener?.onListeningChanged(false)
            onDone()
            return
        }
        runCatching { onSessionClose(live) }
        val bounded = timeoutMs.coerceIn(100L, 5_000L)
        // Cancel any prior pending flush (should not stack).
        pendingTimeout?.let { pendingHandler?.removeCallbacks(it) }
        pendingFinish = null
        pendingHandler = null
        pendingTimeout = null
        pendingDone = null
        try {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val done = java.util.concurrent.atomic.AtomicBoolean(false)
            val finish: () -> Unit = {
                if (done.compareAndSet(false, true)) {
                    pendingTimeout?.let { pendingHandler?.removeCallbacks(it) }
                    pendingFinish = null
                    pendingHandler = null
                    pendingTimeout = null
                    pendingDone = null
                    runCatching { live.close() }
                    listener?.onListeningChanged(false)
                    onDone()
                }
            }
            val timeout = Runnable { finish() }
            pendingFinish = finish
            pendingHandler = handler
            pendingTimeout = timeout
            pendingDone = done
            // Bounded wait, but early finish if a final arrives (see onText) or error (see onError).
            handler.postDelayed(timeout, bounded)
        } catch (_: Exception) {
            pendingFinish = null
            pendingHandler = null
            pendingTimeout = null
            pendingDone = null
            runCatching { live.close() }
            listener?.onListeningChanged(false)
            onDone()
        }
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
        val isTee = teeGeneration != -1
        try {
            if (!isTee) pcm.stop()
            session?.close()
            val live = socket.connect(
                url = connectUrl(languageTag),
                headers = authHeaders(key),
                onError = { err ->
                    if (!isTee) pcm.stop()
                    session = null
                    if (isTee) teeGeneration = -1
                    // Do not emit listening=false first — service treats that as end-of-utterance
                    // and can stop before onError toast/log runs.
                    listener?.onError(err, true)
                    // Flush pending: an error is terminal — complete early instead of waiting full timeout.
                    // Races with timeout are guarded by atomic done in finish().
                    pendingFinish?.let { f ->
                        // Post to handler to keep onDone on main; fallback to direct if handler missing.
                        val h = pendingHandler
                        if (h != null) h.post { f.invoke() } else f.invoke()
                    }
                },
                onText = { msg ->
                    val u = parse(msg) ?: return@connect
                    if (u.text.isBlank()) return@connect
                    if (u.final) {
                        listener?.onFinal(u.text)
                        // Early-complete a pending flush when the trailing final arrives.
                        // Stale callbacks after already finished are ignored via atomic guard.
                        pendingFinish?.let { f ->
                            val h = pendingHandler
                            if (h != null) h.post { f.invoke() } else f.invoke()
                        }
                    } else {
                        listener?.onPartial(u.text)
                    }
                },
            )
            session = live
            onSessionOpen(live)
            if (isTee) {
                // Tee owns the mic; do not start internal PcmSource. AppAudioCapture will feed via feedTeePcm.
                listener?.onReady()
                listener?.onListeningChanged(true)
                return
            }
            val micOn = pcm.start { chunk ->
                if (chunk.isNotEmpty()) writeAudio(live, chunk)
            }
            if (!micOn) {
                pcm.stop()
                session = null
                runCatching { onSessionClose(live) }
                live.close()
                listener?.onError("Microphone start failed", true)
                return
            }
            listener?.onReady()
            listener?.onListeningChanged(true)
        } catch (e: Exception) {
            if (!isTee) pcm.stop()
            session = null
            if (isTee) teeGeneration = -1
            listener?.onError(e.message ?: "cloud ear failed", true)
        }
    }

    protected open fun writeAudio(session: CloudSession, pcm: ByteArray) {
        session.send(pcm)
    }

    protected open fun onSessionOpen(session: CloudSession) = Unit

    protected open fun onSessionClose(session: CloudSession) = Unit
}
