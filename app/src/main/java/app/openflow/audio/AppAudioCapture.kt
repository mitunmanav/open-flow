package app.openflow.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import app.openflow.stt.providers.cloud.WavPcm
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * M3-A single owner for app-owned ears (cloud + on_phone).
 * Owns exactly one AudioRecord, fans out PCM to consumers.
 * Hard invariant: NO PCM from generation g reaches consumers of g+1.
 */
class AppAudioCapture(
    private val context: Context? = null,
    private val requestedRate: Int = 16_000,
    private val maxBytes: Long = CaptureCap.DEFAULT_MAX_BYTES,
) {
    interface PcmConsumer {
        fun onPcm(pcm: ByteArray, generation: Int)
        fun onError(generation: Int) = Unit
    }

    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    private var record: AudioRecord? = null
    private val consumers = CopyOnWriteArrayList<PcmConsumer>()

    @Volatile private var activeGeneration: Int = -1
    @Volatile private var selectedRate: Int = requestedRate
    @Volatile private var selectedSource: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION
    @Volatile private var selectedBufferSize: Int = 0
    @Volatile private var selectedSourceName: String = "VOICE_RECOGNITION"

    fun selectedSourceName(): String = selectedSourceName
    fun selectedRate(): Int = selectedRate
    fun selectedBufferSize(): Int = selectedBufferSize
    fun activeGeneration(): Int = activeGeneration

    @SuppressLint("MissingPermission")
    fun start(generation: Int, newConsumers: List<PcmConsumer>): Boolean {
        stop()
        if (newConsumers.isEmpty()) {
            activeGeneration = generation
            consumers.clear()
            consumers.addAll(newConsumers)
            // No consumers still needs a valid AudioRecord for measurement? But don't open mic if no one needs PCM.
            // For M3-A, cloud/on_phone always have at least one consumer. If none, succeed without mic.
            return true
        }
        val minPrimary = try {
            AudioRecord.getMinBufferSize(requestedRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        } catch (_: Exception) { -1 }

        val framesPerBuffer = try {
            (context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
                ?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull()
        } catch (_: Exception) { null }
        val bytesHint = if (framesPerBuffer != null && framesPerBuffer > 0) framesPerBuffer * 2 else 0
        val floor = requestedRate / 10 * 2

        var chosenRate = requestedRate
        var chosenSource = MediaRecorder.AudioSource.VOICE_RECOGNITION
        var chosenSourceName = "VOICE_RECOGNITION"
        var chosenBuffer = -1
        var ar: AudioRecord? = null

        fun tryCreate(rate: Int, source: Int, sourceName: String): AudioRecord? {
            val m = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (m <= 0) return null
            val bs = maxOf(m, bytesHint, floor)
            val rec = try {
                AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bs)
            } catch (_: Exception) { null } ?: return null
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { rec.release() }
                return null
            }
            chosenRate = rate
            chosenSource = source
            chosenSourceName = sourceName
            chosenBuffer = bs
            return rec
        }

        // Primary: requestedRate with VOICE_RECOGNITION then MIC
        if (minPrimary > 0) {
            ar = tryCreate(requestedRate, MediaRecorder.AudioSource.VOICE_RECOGNITION, "VOICE_RECOGNITION")
                ?: tryCreate(requestedRate, MediaRecorder.AudioSource.MIC, "MIC")
        }
        // Fallback native rates if primary failed
        if (ar == null) {
            for (rate in listOf(48_000, 44_100, 22_050)) {
                if (rate == requestedRate) continue
                ar = tryCreate(rate, MediaRecorder.AudioSource.VOICE_RECOGNITION, "VOICE_RECOGNITION")
                    ?: tryCreate(rate, MediaRecorder.AudioSource.MIC, "MIC")
                if (ar != null) break
            }
        }
        if (ar == null) return false

        record = ar
        selectedRate = chosenRate
        selectedSource = chosenSource
        selectedSourceName = chosenSourceName
        selectedBufferSize = chosenBuffer
        activeGeneration = generation
        consumers.clear()
        consumers.addAll(newConsumers)
        running.set(true)
        try {
            ar.startRecording()
        } catch (_: Exception) {
            running.set(false)
            activeGeneration = -1
            consumers.clear()
            runCatching { ar.release() }
            record = null
            return false
        }
        if (ar.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            running.set(false)
            activeGeneration = -1
            consumers.clear()
            runCatching { ar.stop() }
            runCatching { ar.release() }
            record = null
            return false
        }

        val bufSize = chosenBuffer
        val localRecord = ar
        worker = thread(name = "openflow-app-pcm", isDaemon = true) {
            val buf = ByteArray(bufSize)
            while (running.get()) {
                val n = try { localRecord.read(buf, 0, buf.size) } catch (_: Exception) { break }
                if (n > 0) {
                    val gen = activeGeneration
                    if (gen == -1) continue
                    val raw = buf.copyOf(n)
                    val toDispatch = if (chosenRate != requestedRate) {
                        PcmResampler.resamplePcm16(raw, chosenRate, requestedRate)
                    } else raw
                    if (toDispatch.isEmpty()) continue
                    for (c in consumers) {
                        try {
                            c.onPcm(toDispatch, gen)
                        } catch (_: Exception) { }
                    }
                }
                if (n < 0) {
                    val gen = activeGeneration
                    if (gen != -1) {
                        for (c in consumers) try { c.onError(gen) } catch (_: Exception) {}
                    }
                    break
                }
            }
        }
        return true
    }

    fun stop() {
        running.set(false)
        activeGeneration = -1
        runCatching { worker?.join(500) }
        worker = null
        val ar = record
        record = null
        runCatching { ar?.stop() }
        runCatching { ar?.release() }
        consumers.clear()
    }

    fun stopAndDiscard() = stop()

    fun consumerCount(): Int = consumers.size
}

/**
 * WavFileConsumer — buffers PCM under CaptureCap, writes WAV only for matching generation.
 * System ear must not create this consumer (M3-A). Retention gates creation upstream.
 */
class WavFileConsumer(
    private val startGeneration: Int,
    private val sampleRate: Int = 16_000,
    private val maxBytes: Long = CaptureCap.DEFAULT_MAX_BYTES,
) : AppAudioCapture.PcmConsumer {
    private val chunks = ArrayList<ByteArray>()
    private var retained = 0L
    private val lock = Any()
    @Volatile private var activeGen: Int = startGeneration

    fun clearForNewGeneration(nextGen: Int) {
        synchronized(lock) {
            chunks.clear()
            retained = 0L
            activeGen = nextGen
        }
    }

    override fun onPcm(pcm: ByteArray, generation: Int) {
        if (generation != activeGen) return
        if (pcm.isEmpty()) return
        synchronized(lock) {
            if (generation != activeGen) return
            if (CaptureCap.admit(retained, pcm.size, maxBytes)) {
                chunks.add(pcm.copyOf())
                retained += pcm.size
            }
        }
    }

    fun stopAndWrite(out: File, requestGeneration: Int): File? {
        if (requestGeneration != activeGen) {
            synchronized(lock) { chunks.clear(); retained = 0L }
            return null
        }
        val all: ByteArray? = synchronized(lock) {
            val total = chunks.sumOf { it.size }
            if (total <= 0) {
                chunks.clear()
                retained = 0L
                null
            } else {
                val a = ByteArray(total)
                var o = 0
                for (c in chunks) { System.arraycopy(c, 0, a, o, c.size); o += c.size }
                chunks.clear()
                retained = 0L
                a
            }
        } ?: return null
        return try {
            out.parentFile?.mkdirs()
            out.writeBytes(WavPcm.wrapPcm16leMono(all!!, sampleRate))
            if (out.exists() && out.length() > 44L) out else null
        } catch (_: Exception) { null }
    }

    fun discard() {
        synchronized(lock) { chunks.clear(); retained = 0L }
    }

    fun retainedBytes(): Long = synchronized(lock) { retained }
}

/**
 * Simple linear resampler for PCM16 mono. Avoids poor truncation, handles 48k->16k 3:1 and arbitrary.
 */
object PcmResampler {
    fun resamplePcm16(pcm: ByteArray, fromRate: Int, toRate: Int): ByteArray {
        if (fromRate == toRate) return pcm
        if (pcm.isEmpty() || pcm.size < 2) return ByteArray(0)
        val inSamples = pcm.size / 2
        if (inSamples == 0) return ByteArray(0)
        val outSamples = ((inSamples.toLong() * toRate) / fromRate).toInt().coerceAtLeast(1)
        val out = ByteArray(outSamples * 2)
        // Convert input to short array on fly with interpolation.
        // For performance, do linear interpolation per output sample.
        for (i in 0 until outSamples) {
            val srcPos = i.toDouble() * fromRate / toRate
            val srcIdx = srcPos.toInt().coerceIn(0, inSamples - 1)
            val frac = (srcPos - srcIdx).toFloat()
            val s0 = readLe16(pcm, srcIdx * 2)
            val s1 = if (srcIdx + 1 < inSamples) readLe16(pcm, (srcIdx + 1) * 2) else s0
            val interp = if (frac == 0f) s0 else (s0 + (s1 - s0) * frac).toInt().coerceIn(-32768, 32767)
            out[i * 2] = (interp and 0xff).toByte()
            out[i * 2 + 1] = ((interp shr 8) and 0xff).toByte()
        }
        return out
    }

    private fun readLe16(buf: ByteArray, off: Int): Int {
        val v = (buf[off].toInt() and 0xff) or (buf[off + 1].toInt() shl 8)
        return if (v >= 0x8000) v - 0x10000 else v
    }
}
