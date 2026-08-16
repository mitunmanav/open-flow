package app.openflow.stt.providers.cloud

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 16-bit mono PCM → WAV container so Sarvam `encoding=audio/wav` matches the bytes. */
object WavPcm {
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
