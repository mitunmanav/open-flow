package app.openflow.bubble

/** Overlay listen/chip chrome. 48dp min touch; bar fits action + pad. */
object BubbleTouch {
    const val ACTION_DP = 48
    const val CHIP_MIN_DP = 48
    const val PAD_V_DP = 4
    const val PAD_H_DP = 8
    const val GAP_DP = 8
    const val LISTEN_BAR_DP = ACTION_DP + PAD_V_DP * 2

    /** Listen row: one disc per visible chrome (wave always). */
    fun listenWidthDp(cancel: Boolean = true, done: Boolean = true): Int {
        val n = 1 + (if (cancel) 1 else 0) + (if (done) 1 else 0)
        return n * ACTION_DP + (n - 1).coerceAtLeast(0) * GAP_DP
    }
}
