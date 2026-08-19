package app.openflow.bubble

/** Listen row is three discs. Never scale the whole overlay from RMS. */
object BubbleListenMotion {
    fun overlayScale(base: Float, @Suppress("UNUSED_PARAMETER") rms: Float): Float = base
}
