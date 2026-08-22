package app.openflow.whisper

import kotlin.math.abs

object PcmTrim {
    fun trim(
        samples: FloatArray,
        floor: Float = 0.01f,
        minKeep: Int = 8_000,
    ): FloatArray {
        if (samples.isEmpty()) return samples
        var start = 0
        while (start < samples.size && abs(samples[start]) < floor) start++
        var end = samples.size - 1
        while (end >= start && abs(samples[end]) < floor) end--
        val keep = end - start + 1
        if (keep < minKeep) return samples
        return samples.copyOfRange(start, end + 1)
    }
}
