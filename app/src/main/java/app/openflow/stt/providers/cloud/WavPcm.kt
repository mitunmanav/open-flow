package app.openflow.stt.providers.cloud

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 16-bit mono PCM ↔ WAV container. Wrap for Sarvam `encoding=audio/wav`; unwrap for on_phone replay. */
object WavPcm {
    fun unwrapPcm16leMono(wav: ByteArray): ByteArray? {
        if (wav.size <= 44) return null
        // Minimal RIFF/WAVE check; then skip 44-byte header (PCM mono 16k assumed).
        val head = wav.copyOfRange(0, 12).toString(Charsets.US_ASCII)
        if (!head.startsWith("RIFF") || !head.contains("WAVE")) return null
        return wav.copyOfRange(44, wav.size)
    }

    fun pcm16ToFloat(pcm: ByteArray): FloatArray {
        if (pcm.isEmpty()) return FloatArray(0)
        val n = pcm.size / 2
        val out = FloatArray(n)
        var i = 0
        var o = 0
        while (i + 1 < pcm.size) {
            val v = (pcm[i].toInt() and 0xff) or (pcm[i + 1].toInt() shl 8)
            val signed = if (v >= 0x8000) v - 0x10000 else v
            out[o++] = signed / 32768f
            i += 2
        }
        return if (o == n) out else out.copyOf(o)
    }

    fun wrapPcm16leMono(pcm: ByteArray, sampleRate: Int = 16_000): ByteArray {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        val dataSize = pcm.size
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(1) // mono
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2) // block align
        header.putShort(16) // bits
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)
        return header.array() + pcm
    }
}
