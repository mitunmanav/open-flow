package app.openflow.bubble

/** Command row on the bubble. Visible only when command is enabled. */
object CommandChrome {
    fun visible(commandEnabled: Boolean): Boolean = commandEnabled

    fun suffix(commandEnabled: Boolean): String = if (commandEnabled) " cmd" else ""
}
