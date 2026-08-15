package app.openflow.bubble

/** RMS → lastRms helpers. WaveformBars + listen pulse. */
object BubbleRms {
    fun capture(rmsdB: Float): Float = rmsdB.coerceIn(0f, 10f)

    fun bars(rms: Float): String = WaveformBars.fromRms(capture(rms))

    fun pulseScale(rms: Float): Float = 1f + (capture(rms) / 10f) * 0.2f
}
