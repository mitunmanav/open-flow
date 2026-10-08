package dev.openflow.dictation.providers.sherpa.benchmark

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The eval set's audio reader, on the files a fixed eval set actually contains.
 *
 * The unforgiving behaviour is the point, and it is tested as behaviour rather than as an
 * accident: `model-selection.md` fixes a **16 kHz mono** set, and a stereo capture or a 44.1 kHz
 * download would decode fine and be measured at the wrong rate, producing an RTF that is a
 * property of a resampler rather than of the model.
 */
class WavReaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    /**
     * A minimal RIFF/WAVE writer, including the odd-size chunk padding.
     *
     * `extraFiller` inserts a `LIST`-style chunk whose payload has an odd length, which is the
     * case a naive parser gets wrong and then reports a perfectly good WAV as broken.
     */
    private fun wav(
        samples: FloatArray,
        sampleRate: Int = 16_000,
        channels: Int = 1,
        bitsPerSample: Int = 16,
        oddSizedChunkFirst: Boolean = false,
    ): ByteArray {
        val bytesPerSample = bitsPerSample / 8
        val data = ByteBuffer.allocate(samples.size * bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            when (bitsPerSample) {
                16 -> data.putShort((sample * 32767f).toInt().toShort())
                32 -> data.putFloat(sample)
                // Only used to build a file the reader must reject, so the bytes need not be a
                // faithful 8-bit encoding — only the header has to claim 8.
                else -> data.put((sample * 127f).toInt().toByte())
            }
        }
        val dataBytes = data.array()

        val fmt = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            .putShort(if (bitsPerSample == 32) 3 else 1)
            .putShort(channels.toShort())
            .putInt(sampleRate)
            .putInt(sampleRate * channels * bytesPerSample)
            .putShort((channels * bytesPerSample).toShort())
            .putShort(bitsPerSample.toShort())
            .array()

        val out = java.io.ByteArrayOutputStream()
        fun chunk(id: String, body: ByteArray) {
            out.write(id.toByteArray(Charsets.US_ASCII))
            val size = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(body.size).array()
            out.write(size)
            out.write(body)
            if (body.size % 2 == 1) out.write(0)
        }

        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        val placeholder = ByteArray(4)
        out.write(placeholder)
        out.write("WAVE".toByteArray(Charsets.US_ASCII))
        if (oddSizedChunkFirst) chunk("JUNK", ByteArray(3))
        chunk("fmt ", fmt)
        chunk("data", dataBytes)

        val bytes = out.toByteArray()
        val riffSize = bytes.size - 8
        ByteBuffer.wrap(bytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(riffSize)
        return bytes
    }

    private fun write(name: String, bytes: ByteArray): File =
        folder.newFile(name).apply { writeBytes(bytes) }

    // ---- the happy paths ------------------------------------------------------------

    @Test
    fun sixteenBitPcmIsReadAsFloats() {
        val file = write("a.wav", wav(floatArrayOf(0f, 0.5f, -0.5f, 1f)))
        val samples = WavReader.read(file)
        assertEquals(4, samples.size)
        assertEquals(0f, samples[0], 1e-4f)
        assertEquals(0.5f, samples[1], 1e-4f)
        assertEquals(-0.5f, samples[2], 1e-4f)
        assertEquals(1f, samples[3], 1e-4f)
    }

    @Test
    fun thirtyTwoBitFloatIsReadUnchanged() {
        val file = write("b.wav", wav(floatArrayOf(0.25f, -0.75f), bitsPerSample = 32))
        val samples = WavReader.read(file)
        assertEquals(0.25f, samples[0], 1e-6f)
        assertEquals(-0.75f, samples[1], 1e-6f)
    }

    @Test
    fun anOddSizedChunkBeforeTheDataDoesNotDerailTheParse() {
        // RIFF chunks are word-aligned and an odd-sized one is followed by a pad byte that is
        // not part of the next header. Getting that wrong makes a valid WAV unreadable.
        val file = write("c.wav", wav(floatArrayOf(0.1f, 0.2f), oddSizedChunkFirst = true))
        assertEquals(2, WavReader.read(file).size)
    }

    @Test
    fun theExpectedFormatIsSixteenKilohertzMono() {
        // Stated here as well as enforced, so the constant and the plan cannot drift apart
        // silently.
        assertEquals(16_000, WavReader.EXPECTED_SAMPLE_RATE)
        assertEquals(1, WavReader.EXPECTED_CHANNELS)
    }

    // ---- the rejections -------------------------------------------------------------

    @Test
    fun stereoIsRejectedRatherThanDownmixed() {
        val file = write("stereo.wav", wav(floatArrayOf(0f, 0f), channels = 2))
        try {
            WavReader.read(file)
            fail("expected a WavFormatException")
        } catch (e: WavReader.WavFormatException) {
            assertTrue(e.message!!.contains("2 channels"))
            assertTrue(e.message!!.contains("16 kHz MONO"))
        }
    }

    @Test
    fun theWrongSampleRateIsRejectedRatherThanResampled() {
        val file = write("44k.wav", wav(floatArrayOf(0f), sampleRate = 44_100))
        try {
            WavReader.read(file)
            fail("expected a WavFormatException")
        } catch (e: WavReader.WavFormatException) {
            assertTrue(e.message!!.contains("44100 Hz"))
            assertTrue(e.message!!.contains("16 kHz"))
        }
    }

    @Test
    fun aFileThatIsNotAWavIsRejected() {
        val file = write("notes.wav", "this is a transcript, not audio".toByteArray())
        try {
            WavReader.read(file)
            fail("expected a WavFormatException")
        } catch (e: WavReader.WavFormatException) {
            assertTrue(e.message!!.contains("RIFF"))
        }
    }

    @Test
    fun aRiffThatIsNotWaveIsRejected() {
        val bytes = wav(floatArrayOf(0f)).copyOf()
        bytes[8] = 'A'.code.toByte()
        bytes[9] = 'V'.code.toByte()
        bytes[10] = 'I'.code.toByte()
        bytes[11] = 'F'.code.toByte()
        val file = write("avi.wav", bytes)
        try {
            WavReader.read(file)
            fail("expected a WavFormatException")
        } catch (e: WavReader.WavFormatException) {
            assertTrue(e.message!!.contains("not WAVE"))
        }
    }

    @Test
    fun aCompressedWavIsRejectedRatherThanDecoded() {
        val file = write("compressed.wav", wav(floatArrayOf(0f), bitsPerSample = 8))
        try {
            WavReader.read(file)
            fail("expected a WavFormatException")
        } catch (e: WavReader.WavFormatException) {
            assertTrue(e.message!!.contains("8 bits per sample"))
        }
    }

    @Test
    fun aMissingFileNamesItself() {
        try {
            WavReader.read(File(folder.root, "absent.wav"))
            fail("expected a WavFormatException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("absent.wav"))
        }
    }

    @Test
    fun aFileWithNoDataChunkIsRejected() {
        val bytes = wav(floatArrayOf(0f, 0f))
        val text = String(bytes, Charsets.ISO_8859_1)
        val hacked = text.replace("data", "junk").toByteArray(Charsets.ISO_8859_1)
        val file = write("nodata.wav", hacked)
        try {
            WavReader.read(file)
            fail("expected a WavFormatException")
        } catch (e: WavReader.WavFormatException) {
            assertTrue(e.message!!.contains("no 'data' chunk"))
        }
    }

    @Test
    fun aTruncatedFileIsRejectedRatherThanReadAsSilence() {
        // The alternative — reading what is there — would decode a clipped utterance and score
        // it as though the speaker had stopped early.
        val file = write("short.wav", ByteArray(20))
        try {
            WavReader.read(file)
            fail("expected a WavFormatException")
        } catch (e: WavReader.WavFormatException) {
            assertTrue(e.message!!.contains("too short"))
        }
    }
}
