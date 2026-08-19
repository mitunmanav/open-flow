package app.openflow.stt.providers.ondevice

import app.openflow.stt.SpeechEngine
import app.openflow.whisper.PcmSource
import app.openflow.whisper.WhisperRuntime
import java.io.File

/** On-phone whisper.cpp ear. Missing model → fatal. Transcribes on [stopAndFlush]. */
class OnDeviceEar(
    private val modelFile: File? = null,
    private val micGranted: Boolean = false,
    private val runtime: WhisperRuntime? = null,
    private val pcm: PcmSource? = null,
    private val runWork: (() -> Unit) -> Unit = { it() },
    private val onMain: (() -> Unit) -> Unit = { it() },
) : SpeechEngine {

    companion object {
        const val FLUSH_TIMEOUT_MS = 30_000L
    }

    private var listener: SpeechEngine.Listener? = null
    private var listening = false

    override val isAvailable: Boolean
        get() = modelFile?.isFile == true || runtime?.isLoaded() == true

    override fun hasMicPermission(): Boolean = micGranted

    override fun setListener(listener: SpeechEngine.Listener?) {
        this.listener = listener
    }

    override fun startContinuous(languageTag: String) {
        start()
    }

    override fun startOnce(languageTag: String) {
        start()
    }

    override fun stop() {
        if (!listening) return
        listening = false
        listener?.onListeningChanged(false)
    }

    override fun stopAndFlush(timeoutMs: Long, onDone: () -> Unit) {
        if (!modelReady() || runtime == null) {
            stop()
            onDone()
            return
        }
        runWork {
            val samples = pcm?.take() ?: FloatArray(0)
            val text = if (samples.isNotEmpty()) runtime.transcribe(samples).trim() else ""
            onMain {
                stop()
                if (text.isNotEmpty()) listener?.onFinal(text)
                onDone()
            }
        }
    }

    override fun destroy() {
        stop()
        runtime?.release()
        listener = null
    }

    fun emitRms(db: Float) {
        listener?.onRmsChanged(db)
    }

    private fun start() {
        if (!modelReady()) {
            listener?.onError("model not installed", fatal = true)
            return
        }
        pcm?.start()
        listening = true
        listener?.onReady()
        listener?.onListeningChanged(true)
    }

    private fun modelReady(): Boolean =
        modelFile?.isFile == true || runtime != null
}
