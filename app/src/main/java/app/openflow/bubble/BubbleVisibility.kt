package app.openflow.bubble

/**
 * Wispr-style bubble show rules (pure).
 *
 * Android Flow Bubble appears when an editable field is in play;
 * optional IME gate matches "sits above keyboard" behavior.
 */
object BubbleVisibility {

    /**
     * @param snoozed user drag-to-bottom snooze
     * @param bankHide package denylist (banks etc.)
     * @param hasEditable focused usable text field
     * @param imeVisible soft keyboard window present (optional; if unknown pass true)
     * @param alwaysShow when true (debug/settings), show even without field
     */
    fun shouldShow(
        snoozed: Boolean,
        bankHide: Boolean,
        hasEditable: Boolean,
        imeVisible: Boolean = true,
        alwaysShow: Boolean = false
    ): Boolean {
        if (snoozed || bankHide) return false
        if (alwaysShow) return true
        return hasEditable && imeVisible
    }
}
