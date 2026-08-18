package app.openflow.bubble

/**
 * Wispr-style bubble show rules (pure).
 *
 * Keyboard open + editable field: stay visible even if snoozed.
 * Own app UI hides the overlay unless listening / must-stay.
 */
object BubbleVisibility {

    fun shouldShow(
        snoozed: Boolean,
        bankHide: Boolean,
        hasEditable: Boolean,
        imeVisible: Boolean = true,
        alwaysShow: Boolean = false,
        listening: Boolean = false,
        insideOwnApp: Boolean = false,
        mustStay: Boolean = false,
    ): Boolean {
        if (bankHide) return false
        if (insideOwnApp && !listening && !mustStay && !alwaysShow) return false
        if (listening || mustStay || alwaysShow) return true
        if (imeVisible && hasEditable) return true
        if (snoozed) return false
        return hasEditable && imeVisible
    }
}
