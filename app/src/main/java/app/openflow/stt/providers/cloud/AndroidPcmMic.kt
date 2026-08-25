package app.openflow.stt.providers.cloud

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** 16 kHz mono PCM16 mic. Fail-soft if AudioRecord cannot start. */
class AndroidPcmMic(
    private val sampleRate: Int = 16_000,
) : PcmSource {

    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    private var record: AudioRecord? = null

    // RECORD_AUDIO is checked upstream (service refuses listen without it);
    // AudioRecord construction fails soft via try/catch if permission missing.
    @SuppressLint("MissingPermission")
    override fun start(onChunk: (ByteArray) -> Unit): Boolean {
        stop()
        val min = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (!PcmStartPolicy.bufferOk(min)) return false
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
            return false
        }
        if (!PcmStartPolicy.recordOk(ar.state == AudioRecord.STATE_INITIALIZED)) {
            ar.release()
            return false
        }
        record = ar
        running.set(true)
        try {
            ar.startRecording()
        } catch (_: Exception) {
            running.set(false)
            ar.release()
            record = null
            return false
        }
        worker = thread(name = "openflow-pcm", isDaemon = true) {
            val buf = ByteArray(bufSize)
            while (running.get()) {
                val n = try {
                    ar.read(buf, 0, buf.size)
                } catch (_: Exception) {
                    break
                }
                if (n > 0) onChunk(buf.copyOf(n))
                if (n < 0) break
            }
        }
        return true
    }

    override fun stop() {
        running.set(false)
        runCatching { worker?.join(500) }
        worker = null
        val ar = record
        record = null
        runCatching { ar?.stop() }
        runCatching { ar?.release() }
    }
}
