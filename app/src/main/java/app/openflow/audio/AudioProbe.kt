package app.openflow.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock

/**
 * Device-aware audio probe for M3 measurement gate.
 * Records requested/actual sample rate, minBuffer, selected buffer,
 * framesPerBuffer, source, state, startRecording result, first PCM timing
 * and fallback/resample need — per M3 rev2 spec.
 */
data class AudioProbeResult(
    val requestedRate: Int,
    val actualRate: Int,
    val minBufferSize: Int,
    val selectedBufferSize: Int,
    val framesPerBuffer: Int?,
    val source: String,
    val state: String,
    val startRecordingOk: Boolean,
    val fallbackUsed: Boolean,
    val fallbackRate: Int?,
    val resampleNeeded: Boolean,
    val firstPcmMs: Long?,
    val error: String?,
    val elapsedMs: Long,
)

object AudioProbe {
    private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    fun probe(context: Context?, requestedRate: Int = 16_000): AudioProbeResult {
        val t0 = SystemClock.elapsedRealtime()
        val framesPerBuffer = try {
            (context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
                ?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull()
        } catch (_: Exception) { null }

        val min = try {
            AudioRecord.getMinBufferSize(requestedRate, CHANNEL, ENCODING)
        } catch (e: Exception) { -1 }

        val candidates = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
            MediaRecorder.AudioSource.MIC to "MIC",
        )
        var selectedSource = "none"
        var selectedRate = requestedRate
        var selectedBuf = -1
        var stateStr = "NOT_CREATED"
        var actualRate = -1
        var startOk = false
        var firstMs: Long? = null
        var fallbackUsed = false
        var fallbackRate: Int? = null
        var error: String? = null
        var record: AudioRecord? = null

        val bufHint = if (framesPerBuffer != null && framesPerBuffer > 0) framesPerBuffer * 2 else 0
        val floor = requestedRate / 10 * 2 // 100 ms

        fun tryRate(rate: Int, labelFallback: Boolean): Boolean {
            val m = AudioRecord.getMinBufferSize(rate, CHANNEL, ENCODING)
            if (m <= 0) return false
            val bs = maxOf(m, bufHint, floor)
            for ((src, name) in candidates) {
                val ar = try {
                    AudioRecord(src, rate, CHANNEL, ENCODING, bs)
                } catch (e: Exception) {
                    error = e.message
                    null
                } ?: continue
                stateStr = if (ar.state == AudioRecord.STATE_INITIALIZED) "INITIALIZED" else "UNINITIALIZED:${ar.state}"
                if (ar.state != AudioRecord.STATE_INITIALIZED) {
                    runCatching { ar.release() }
                    continue
                }
                record = ar
                selectedSource = name
                selectedRate = rate
                selectedBuf = bs
                actualRate = try { ar.sampleRate } catch (_: Exception) { rate }
                fallbackUsed = labelFallback
                if (labelFallback) fallbackRate = rate
                return true
            }
            return false
        }

        val okPrimary = tryRate(requestedRate, false)
        if (!okPrimary) {
            for (fallback in listOf(48_000, 44_100, 22_050)) {
                if (fallback == requestedRate) continue
                if (tryRate(fallback, true)) break
            }
        }

        if (record != null) {
            try {
                record!!.startRecording()
                startOk = record!!.recordingState == AudioRecord.RECORDSTATE_RECORDING
                if (startOk) {
                    val buf = ByteArray(selectedBuf.coerceAtLeast(2048))
                    val deadline = SystemClock.elapsedRealtime() + 800
                    while (SystemClock.elapsedRealtime() < deadline) {
                        val n = try { record!!.read(buf, 0, buf.size) } catch (_: Exception) { -1 }
                        if (n > 0) { firstMs = SystemClock.elapsedRealtime() - t0; break }
                        if (n < 0) { error = "read $n"; break }
                    }
                }
            } catch (e: Exception) {
                error = e.message
                startOk = false
            } finally {
                runCatching { record!!.stop() }
                runCatching { record!!.release() }
            }
        } else if (error == null) {
            error = "no viable AudioRecord"
            stateStr = "FAILED"
        }

        val elapsed = SystemClock.elapsedRealtime() - t0
        val resample = selectedRate != requestedRate && selectedRate != -1
        return AudioProbeResult(
            requestedRate = requestedRate,
            actualRate = actualRate,
            minBufferSize = min,
            selectedBufferSize = selectedBuf,
            framesPerBuffer = framesPerBuffer,
            source = selectedSource,
            state = stateStr,
            startRecordingOk = startOk,
            fallbackUsed = fallbackUsed,
            fallbackRate = fallbackRate,
            resampleNeeded = resample,
            firstPcmMs = firstMs,
            error = error,
            elapsedMs = elapsed,
        )
    }
}
