package app.openflow.bubble

object BubbleSnoozePolicy {
    fun canSnooze(imeVisible: Boolean, listening: Boolean, repairShowing: Boolean): Boolean =
        !imeVisible && !listening && !repairShowing
}
