package app.openflow.bubble

/** Command row on the bubble. Visible only when command is enabled. */
object CommandChrome {
    fun visible(commandEnabled: Boolean): Boolean = commandEnabled
}
