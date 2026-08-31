package app.openflow.stt.providers.ondevice

import app.openflow.BuildConfig
import app.openflow.audio.CaptureCap
import app.openflow.stt.SpeechEngine
import app.openflow.stt.providers.cloud.WavPcm
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
    private val chunkSamples: Int = CHUNK_SAMPLES,
) : SpeechEngine {

    companion object {
        const val FLUSH_TIMEOUT_MS = 120_000L
        const val CHUNK_SAMPLES = 16_000 * 15
    }

    private val chunker = PcmChunker(chunkSamples)
    private val parts = TranscriptParts()
    private var listener: SpeechEngine.Listener? = null
    private var listening = false
    // WAV retry buffer — on_phone only, so bubble mic (SessionAudioCapture) does not need to run.
    // Kept under CaptureCap so long sessions do not OOM. Guarded by synchronized(wavChunks).
    private val wavChunks = mutableListOf<ByteArray>()
    private var wavRetained = 0L
    // M3-A tee: when >=0, PCM is fed via feedTeePcm and internal PcmSource is not used for capture.
    // Internal wavChunks buffering is disabled in tee mode; WavFileConsumer owns retry WAV.
    @Volatile private var teeGeneration: Int = -1

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

    /** M3-A: set tee generation before start; -1 disables tee and uses internal PcmSource. */
    fun setTeeGeneration(gen: Int) { teeGeneration = gen }

    /** M3-A: feed PCM16 from AppAudioCapture; drops stale generation. */
    fun feedTeePcm(pcmBytes: ByteArray, generation: Int) {
        if (generation != teeGeneration) return
        if (pcmBytes.isEmpty()) return
        if (!listening) return
        val floats = WavPcm.pcm16ToFloat(pcmBytes)
        if (floats.isEmpty()) return
        // Tee mode: WavFileConsumer owns retry WAV, so skip internal wavChunks buffering here.
        // Preserve whisper chunking/transcription exactly as non-tee pcm path.
        val windows = chunker.push(floats)
        if (windows.isEmpty()) return
        runWork { transcribeWindows(windows) }
    }

    fun feedTeePcmError(generation: Int) {
        if (generation != teeGeneration) return
        listener?.onError("Mic audio error", true)
    }

    override fun stop() {
        if (!listening) return
        teeGeneration = -1
        listening = false
        listener?.onListeningChanged(false)
    }

    /** Write the buffered WAV for this session to [out] for retry. Returns file if written. */
    fun writeWav(out: File): File? {
        val pcm = synchronized(wavChunks) {
            val total = wavChunks.sumOf { it.size }
            if (total <= 0) {
                wavChunks.clear()
                wavRetained = 0L
                return null
            }
            val all = ByteArray(total)
            var o = 0
            for (c in wavChunks) {
                System.arraycopy(c, 0, all, o, c.size)
                o += c.size
            }
            wavChunks.clear()
            wavRetained = 0L
            all
        }
        return try {
            out.parentFile?.mkdirs()
            out.writeBytes(WavPcm.wrapPcm16leMono(pcm, 16_000))
            if (out.exists() && out.length() > 44L) out else null
        } catch (_: Exception) {
            null
        }
    }

    fun discardWav() {
        synchronized(wavChunks) {
            wavChunks.clear()
            wavRetained = 0L
        }
    }

    /**
     * Transcribe a previously saved WAV file without re-recording the mic.
     * Used for honest retry: on_phone can replay the file, cloud/system cannot.
     * Runs synchronously on the caller thread — caller should dispatch off main.
     */
    fun transcribeWavFile(file: File): String {
        val rt = runtime ?: return ""
        if (!modelReady()) return ""
        val wavBytes = try { file.readBytes() } catch (_: Exception) { return "" }
        val pcm = WavPcm.unwrapPcm16leMono(wavBytes) ?: return ""
        val floats = WavPcm.pcm16ToFloat(pcm)
        if (floats.isEmpty()) return ""
        val localChunker = PcmChunker(chunkSamples)
        val localParts = TranscriptParts()
        val windows = localChunker.push(floats)
        for (raw in windows) {
            val samples = if (raw.size < CHUNK_SAMPLES) PcmTrim.trim(raw) else raw
            if (samples.isEmpty()) continue
            val text = rt.transcribe(samples).trim()
            localParts.add(text)
        }
        val tail = localChunker.flush()
        if (tail.isNotEmpty()) {
            val samples = PcmTrim.trim(tail)
            if (samples.isNotEmpty()) {
                val text = rt.transcribe(samples).trim()
                localParts.add(text)
            }
        }
        val out = localParts.join()
        if (BuildConfig.DEBUG) {
            val flagged = localParts.last
            if (flagged.loopCollapsed || flagged.signatures.isNotEmpty()) {
                android.util.Log.i(
                    "OpenFlow.Whisper",
                    "replay hallucination loops=${flagged.loopCollapsed} sigs=${flagged.signatures}",
                )
            }
        }
        return out
    }

    override fun stopAndFlush(timeoutMs: Long, onDone: () -> Unit) {
        if (!modelReady() || runtime == null) {
            stop()
            onDone()
            return
        }
        val isTee = teeGeneration != -1
        val bounded = timeoutMs.coerceIn(100L, FLUSH_TIMEOUT_MS)
        val rem = if (isTee) FloatArray(0) else pcm?.take() ?: FloatArray(0)
        val done = java.util.concurrent.atomic.AtomicBoolean(false)
        val finishOnce: (() -> Unit) -> (() -> Unit) = { action ->
            { if (done.compareAndSet(false, true)) action() }
        }
        val timeoutFinish = finishOnce {
            onMain {
                stop()
                val partial = parts.join()
                if (partial.isNotEmpty()) listener?.onFinal(partial)
                parts.clear()
                runCatching { chunker.flush() }
                onDone()
            }
        }
        try {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.postDelayed({ timeoutFinish() }, bounded)
        } catch (_: Exception) {
        }
        runWork {
            transcribeWindows(chunker.push(rem))
            val tail = chunker.flush()
            if (tail.isNotEmpty()) transcribeWindows(listOf(tail))
            val text = parts.join()
            val flagged = parts.last
            if (BuildConfig.DEBUG) {
                if (flagged.loopCollapsed || flagged.signatures.isNotEmpty()) {
                    android.util.Log.i(
                        "OpenFlow.Whisper",
                        "hallucination loops=${flagged.loopCollapsed} sigs=${flagged.signatures}",
                    )
                }
            }
            val complete = finishOnce {
                onMain {
                    stop()
                    if (text.isNotEmpty()) listener?.onFinal(text)
                    parts.clear()
                    onDone()
                }
            }
            complete()
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
        // Fresh WAV buffer for retry — previous session's wav was already written/discarded via writeWav/discardWav.
        synchronized(wavChunks) {
            wavChunks.clear()
            wavRetained = 0L
        }
        // Clear stale transcript state for the new live session.
        runCatching { chunker.flush() }
        parts.clear()
        val isTee = teeGeneration != -1
        if (!isTee) {
            pcm?.start { samples ->
                // Buffer PCM16 for WAV retry (4-min cap). Fail-soft if conversion fails.
                // In tee mode, WavFileConsumer owns WAV — skip internal buffering.
                runCatching {
                    val pcmBytes = floatToPcmBytes(samples)
                    if (pcmBytes.isNotEmpty()) {
                        synchronized(wavChunks) {
                            if (CaptureCap.admit(wavRetained, pcmBytes.size)) {
                                wavChunks.add(pcmBytes)
                                wavRetained += pcmBytes.size
                            }
                        }
                    }
                }
                val windows = chunker.push(samples)
                if (windows.isEmpty()) return@start
                runWork { transcribeWindows(windows) }
            }
        }
        listening = true
        listener?.onReady()
        listener?.onListeningChanged(true)
        runWork { runtime?.preload() }
    }

    private fun floatToPcmBytes(floats: FloatArray): ByteArray {
        if (floats.isEmpty()) return ByteArray(0)
        val out = ByteArray(floats.size * 2)
        var o = 0
        for (f in floats) {
            val v = (f * 32767f).toInt().coerceIn(-32768, 32767)
            out[o++] = (v and 0xff).toByte()
            out[o++] = ((v shr 8) and 0xff).toByte()
        }
        return out
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
