package app.openflow.bubble

/**
 * Register flags for in-app dictation broadcasts.
 * Copy-last and debug inject must not be visible to other apps.
 *
 * Values match [android.content.Context] API 33:
 * RECEIVER_EXPORTED = 0x2, RECEIVER_NOT_EXPORTED = 0x4.
 */
object ReceiverExportPolicy {
    const val NOT_EXPORTED = 0x4

    fun copyFlags(): Int = NOT_EXPORTED

    fun injectFlags(): Int = NOT_EXPORTED

    fun injectAllowed(debug: Boolean): Boolean = debug
}
