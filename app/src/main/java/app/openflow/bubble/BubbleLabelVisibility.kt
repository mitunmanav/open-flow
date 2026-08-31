package app.openflow.bubble

/**
 * When the bubble label TextView may be VISIBLE.
 * Idle/listen respect [showText]; post-stop chips replace the label.
 */
object BubbleLabelVisibility {
    fun idle(showText: Boolean): Boolean = showText
    fun listening(showText: Boolean): Boolean = showText
    fun postStop(): Boolean = false
}
