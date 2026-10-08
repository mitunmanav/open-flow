package dev.openflow.dictation.providers.sherpa.benchmark

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The eval set's audio, read by this harness rather than by sherpa-onnx.
 *
 * **Why not `WaveReader`.** sherpa-onnx ships `WaveReader.readWave(path)`, and it
 * works. It is also a JNI call into the same native library the benchmark is trying to
 * measure, which puts it in the measured path: a run whose audio loader allocates and
 * frees inside the library under test cannot separate the loader's cost from the model's.
 * Worse, the reading is then **untestable without a device** — the very failure this
 * ticket must not ship. A RIFF header is 44 bytes of specification, so it is parsed here,
 * in the same source set as the WER scorer, and it is covered by an ordinary JVM unit
 * test.
 *
 * The parser is deliberately unforgiving. `model-selection.md`'s *Method* fixes a
 * **16 kHz mono** eval set, and the two ways a stray file gets into that directory are a
 * stereo capture and a 44.1 kHz download — both of which would decode correctly and be
 * measured at the wrong sample rate, producing an RTF that is a property of the resampler
 * rather than of the model. A loud failure is recoverable; a plausible wrong RTF is not.
 */
object WavReader {

    /** The sample rate `model-selection.md` fixes for the eval set. */
    const val EXPECTED_SAMPLE_RATE = 16_000

    /** The channel count it fixes. */
    const val EXPECTED_CHANNELS = 1

    /** A WAV the harness cannot score, named loudly enough to fix from the message alone. */
    class WavFormatException(message: String) : IOException(message)

    /**
     * Reads [file] and returns its samples as `FloatArray` in `[-1, 1]`.
     *
     * Throws rather than resampling or downmixing. See the class comment: the harness
     * measures the model, and quietly fixing the audio would move the failure from a
     * visible error to an invisible bias.
     */
    fun read(file: File): FloatArray {
        if (!file.isFile) throw WavFormatException("${file.path} does not exist or is not a file.")
        val bytes = file.readBytes()
        if (bytes.size < 44) throw WavFormatException("${file.name} is ${bytes.size} bytes; too short to be a WAV file.")
        if (string(bytes, 0) != "RIFF") throw WavFormatException("${file.name} does not start with a RIFF header (found '${string(bytes, 0)}').")
        if (string(bytes, 8) != "WAVE") throw WavFormatException("${file.name} is RIFF but not WAVE (found '${string(bytes, 8)}').")

        var format: Int = -1
        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var data: ByteArray? = null

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val id = string(bytes, offset)
            val size = littleEndianInt(bytes, offset + 4)
            val body = offset + 8
            if (size < 0 || body + size > bytes.size) {
                throw WavFormatException("${file.name}: chunk '$id' claims $size bytes at offset $body but the file is ${bytes.size} bytes.")
            }
            when (id) {
                "fmt " -> {
                    if (size < 16) throw WavFormatException("${file.name}: 'fmt ' chunk is $size bytes; the header needs 16.")
                    format = littleEndianShort(bytes, body)
                    channels = littleEndianShort(bytes, body + 2)
                    sampleRate = littleEndianInt(bytes, body + 4)
                    bitsPerSample = littleEndianShort(bytes, body + 14)
                }
                "data" -> data = bytes.copyOfRange(body, body + size)
            }
            // RIFF chunks are word-aligned: an odd-sized chunk is followed by one pad byte
            // that is not part of the next chunk's header. Skipping it incorrectly is the
            // classic way to read a valid WAV as a broken one.
            offset = body + size + (size and 1)
        }

        val payload = data ?: throw WavFormatException("${file.name} has no 'data' chunk. A WAV with no samples cannot be decoded, let alone scored.")
        if (format != PCM && format != IEEE_FLOAT) {
            throw WavFormatException("${file.name}: audio format $format is neither PCM (1) nor IEEE float (3). Compressed WAV cannot be read without resampling it first, which would measure the decoder.")
        }
        if (channels != EXPECTED_CHANNELS) {
            throw WavFormatException(
                "${file.name} has $channels channels. model-selection.md's Method fixes a 16 kHz MONO eval set; " +
                    "downmixing here would measure the downmix."
            )
        }
        if (sampleRate != EXPECTED_SAMPLE_RATE) {
            throw WavFormatException(
                "${file.name} is $sampleRate Hz. model-selection.md's Method fixes 16 kHz; resampling here would " +
                    "measure the resampler, and RTF is reported against the audio duration this harness computed."
            )
        }

        return when (bitsPerSample) {
            16 -> pcm16(payload, file.name)
            32 -> float32(payload, file.name)
            else -> throw WavFormatException("${file.name} is $bitsPerSample bits per sample; only 16-bit PCM and 32-bit float are read.")
        }
    }

    private const val PCM = 1
    private const val IEEE_FLOAT = 3

    private fun pcm16(payload: ByteArray, name: String): FloatArray {
        val count = payload.size / 2
        if (count == 0) throw WavFormatException("$name has an empty 'data' chunk.")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(count) {
            // 32768 rather than 32767: it maps -1 to exactly -1.0 and keeps the range
            // symmetric, which is what a float sample rate convention expects.
            buffer.short / 32768f
        }
    }

    private fun float32(payload: ByteArray, name: String): FloatArray {
        val count = payload.size / 4
        if (count == 0) throw WavFormatException("$name has an empty 'data' chunk.")
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(count) { buffer.float }
    }

    private fun string(bytes: ByteArray, at: Int): String =
        String(bytes, at, 4, Charsets.US_ASCII)

    private fun littleEndianInt(bytes: ByteArray, at: Int): Int =
        ByteBuffer.wrap(bytes, at, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun littleEndianShort(bytes: ByteArray, at: Int): Int =
        ByteBuffer.wrap(bytes, at, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
}
