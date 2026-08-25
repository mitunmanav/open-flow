package app.openflow.bubble

import android.content.Context

/**
 * Register flags for in-app dictation broadcasts.
 * Copy-last and debug inject must not be visible to other apps.
 *
 * [Context.RECEIVER_NOT_EXPORTED] is an API 33 int constant, inlined at
 * compile time; call sites are guarded by SDK_INT >= 33.
 */
object ReceiverExportPolicy {
    const val NOT_EXPORTED = Context.RECEIVER_NOT_EXPORTED

    fun copyFlags(): Int = NOT_EXPORTED

    fun injectFlags(): Int = NOT_EXPORTED

    fun injectAllowed(debug: Boolean): Boolean = debug
}
