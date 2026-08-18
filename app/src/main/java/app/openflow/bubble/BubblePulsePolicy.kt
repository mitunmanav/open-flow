package app.openflow.bubble

object BubblePulsePolicy {
    fun scale(listening: Boolean, pulseOn: Boolean, base: Float, rms: Float): Float =
        if (listening && pulseOn) base * BubbleRms.pulseScale(rms) else base
}
