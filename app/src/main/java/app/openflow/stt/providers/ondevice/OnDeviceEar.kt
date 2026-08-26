package app.openflow.stt.providers.ondevice

import app.openflow.stt.SpeechEngine
import app.openflow.whisper.PcmChunker
import app.openflow.whisper.PcmSource
import app.openflow.whisper.PcmTrim
import app.openflow.whisper.TranscriptParts
import app.openflow.whisper.WhisperRuntime
import java.io.File

/** On-phone whisper.cpp ear. Missing model → fatal. Chunks during listen; text on flush. */
class OnDeviceEar(
    private val modelFile: File? = null,
    private val micGranted: Boolean = false,
    private val runtime: WhisperRuntime? = null,
    private val pcm: PcmSource? = null,
    private val runWork: (() -> Unit) -> Unit = { it() },
    private val onMain: (() -> Unit) -> Unit = { it() },
    chunkSamples: Int = CHUNK_SAMPLES,
) : SpeechEngine {

    companion object {
        const val FLUSH_TIMEOUT_MS = 120_000L
        const val CHUNK_SAMPLES = 16_000 * 15
    }

    private val chunker = PcmChunker(chunkSamples)
    private val parts = TranscriptParts()
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
        val rem = pcm?.take() ?: FloatArray(0)
        runWork {
            transcribeWindows(chunker.push(rem))
            val tail = chunker.flush()
            if (tail.isNotEmpty()) transcribeWindows(listOf(tail))
            val text = parts.join()
            val flagged = parts.last
            if (flagged.loopCollapsed || flagged.signatures.isNotEmpty()) {
                android.util.Log.i(
                    "OpenFlow.Whisper",
                    "hallucination loops=${flagged.loopCollapsed} sigs=${flagged.signatures}",
                )
            }
            onMain {
                stop()
                if (text.isNotEmpty()) listener?.onFinal(text)
                onDone()
            }
        }
    }

    override fun destroy() {
        stop()
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
        pcm?.start { samples ->
            val windows = chunker.push(samples)
            if (windows.isEmpty()) return@start
            runWork { transcribeWindows(windows) }
        }
        listening = true
        listener?.onReady()
        listener?.onListeningChanged(true)
        runWork { runtime?.preload() }
    }

    private fun transcribeWindows(windows: List<FloatArray>) {
        val rt = runtime ?: return
        for (raw in windows) {
            val samples = if (raw.size < CHUNK_SAMPLES) PcmTrim.trim(raw) else raw
            if (samples.isEmpty()) continue
            val text = rt.transcribe(samples).trim()
            parts.add(text)
        }
    }

    private fun modelReady(): Boolean =
        modelFile?.isFile == true || runtime != null
}
