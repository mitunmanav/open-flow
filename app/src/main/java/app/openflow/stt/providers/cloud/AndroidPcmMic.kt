package app.openflow.stt.providers.cloud

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

    override fun start(onChunk: (ByteArray) -> Unit) {
        stop()
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
