package app.openflow.bubble

/** Command row on the bubble. Commands still run; suffix is never shown. */
object CommandChrome {
    fun visible(commandEnabled: Boolean): Boolean = commandEnabled

    @Suppress("UNUSED_PARAMETER")
    fun suffix(commandEnabled: Boolean): String = ""
}
