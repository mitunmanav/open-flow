package app.openflow.audio

import app.openflow.stt.providers.cloud.WavPcm
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Captures 16 kHz mono PCM during a listen session and writes a WAV for retry.
 * Fail-soft: if mic cannot start, [stopAndWrite] returns null.
 */
class SessionAudioCapture(
    private val sampleRate: Int = 16_000,
) {
    private val running = AtomicBoolean(false)
    private val chunks = ArrayList<ByteArray>()
    private var worker: Thread? = null
    private var record: android.media.AudioRecord? = null

    fun start() {
        stopAndDiscard()
        val min = android.media.AudioRecord.getMinBufferSize(
            sampleRate,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
        )
        if (min <= 0) return
        val bufSize = min.coerceAtLeast(sampleRate / 5 * 2)
        val ar = try {
            android.media.AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
            )
        } catch (_: Exception) {
            return
        }
        if (ar.state != android.media.AudioRecord.STATE_INITIALIZED) {
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
        worker = thread(name = "openflow-session-pcm", isDaemon = true) {
            val buf = ByteArray(bufSize)
            while (running.get()) {
                val n = try {
                    ar.read(buf, 0, buf.size)
                } catch (_: Exception) {
                    break
                }
                if (n > 0) {
                    synchronized(chunks) { chunks.add(buf.copyOf(n)) }
                }
                if (n < 0) break
            }
        }
    }

    /** Stop mic and write WAV to [out]. Returns [out] if bytes written, else null. */
    fun stopAndWrite(out: File): File? {
        running.set(false)
        runCatching { worker?.join(800) }
        worker = null
        val ar = record
        record = null
        runCatching { ar?.stop() }
        runCatching { ar?.release() }
        val pcm = synchronized(chunks) {
            val total = chunks.sumOf { it.size }
            if (total <= 0) {
                chunks.clear()
                return null
            }
            val all = ByteArray(total)
            var o = 0
            for (c in chunks) {
                System.arraycopy(c, 0, all, o, c.size)
                o += c.size
            }
            chunks.clear()
            all
        }
        return try {
            out.parentFile?.mkdirs()
            out.writeBytes(WavPcm.wrapPcm16leMono(pcm, sampleRate))
            if (out.exists() && out.length() > 44L) out else null
        } catch (_: Exception) {
            null
        }
    }

    fun stopAndDiscard() {
        running.set(false)
        runCatching { worker?.join(500) }
        worker = null
        val ar = record
        record = null
        runCatching { ar?.stop() }
        runCatching { ar?.release() }
        synchronized(chunks) { chunks.clear() }
    }
}
