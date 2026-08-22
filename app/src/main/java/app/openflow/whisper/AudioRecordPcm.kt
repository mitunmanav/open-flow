package app.openflow.whisper

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** 16 kHz mono PCM16 → float. Streams samples; does not keep the whole session. */
class AudioRecordPcm(
    private val sampleRate: Int = 16_000,
    private val onRms: (Float) -> Unit = {},
) : PcmSource {
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    private var record: AudioRecord? = null
    private var onSamples: (FloatArray) -> Unit = {}

    override fun start(onSamples: (FloatArray) -> Unit) {
        take()
        this.onSamples = onSamples
        val min = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (min <= 0) return
        val bufSize = min.coerceAtLeast(sampleRate / 5 * 2)
        val ar = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
            )
        } catch (_: Exception) {
            return
        }
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            ar.release()
            return
        }
        record = ar
        running.set(true)
        try {
            ar.startRecording()
        } catch (_: Exception) {
            running.set(false)
            ar.release()
            record = null
            return
        }
        worker = thread(name = "openflow-whisper-pcm", isDaemon = true) {
            val buf = ByteArray(bufSize)
            while (running.get()) {
                val n = try {
                    ar.read(buf, 0, buf.size)
                } catch (_: Exception) {
                    break
                }
                if (n > 0) {
                    val f = toFloat(buf, n)
                    onRms(rmsDb(buf, n))
                    if (f.isNotEmpty()) onSamples(f)
                }
                if (n < 0) break
            }
        }
    }

    override fun take(): FloatArray {
        running.set(false)
        runCatching { worker?.join(800) }
        worker = null
        val ar = record
        record = null
        runCatching { ar?.stop() }
        runCatching { ar?.release() }
        onSamples = {}
        return FloatArray(0)
    }

    private fun toFloat(pcm: ByteArray, n: Int): FloatArray {
        val out = FloatArray(n / 2)
        var i = 0
        var s = 0
        while (i + 1 < n) {
            val v = (pcm[i].toInt() and 0xff) or (pcm[i + 1].toInt() shl 8)
            val signed = if (v >= 0x8000) v - 0x10000 else v
            out[s++] = signed / 32768f
            i += 2
        }
        return if (s == out.size) out else out.copyOf(s)
    }

    private fun rmsDb(buf: ByteArray, n: Int): Float {
        var sum = 0.0
        var i = 0
        var count = 0
        while (i + 1 < n) {
            val v = (buf[i].toInt() and 0xff) or (buf[i + 1].toInt() shl 8)
            val signed = if (v >= 0x8000) v - 0x10000 else v
            sum += signed.toDouble() * signed
            count++
            i += 2
        }
        if (count == 0) return 0f
        val rms = kotlin.math.sqrt(sum / count)
        return (20.0 * kotlin.math.log10((rms / 32768.0).coerceAtLeast(1e-6))).toFloat()
    }
}
