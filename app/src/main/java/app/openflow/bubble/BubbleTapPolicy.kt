package app.openflow.bubble

object BubbleTapPolicy {
    enum class Action { START, STOP_SAVE, STOP_DISCARD, COPY, UNDO, PASTE, NONE }

    fun action(
        listening: Boolean,
        stopInProgress: Boolean,
        dragged: Boolean,
        longPressFired: Boolean,
        hitCancel: Boolean,
        hitDone: Boolean,
        cancelled: Boolean = false,
        hitCopy: Boolean = false,
        hitUndo: Boolean = false,
        hitPaste: Boolean = false,
    ): Action {
        if (cancelled || dragged || stopInProgress) return Action.NONE
        if (!listening) {
            if (hitCopy) return Action.COPY
            if (hitUndo) return Action.UNDO
            if (hitPaste) return Action.PASTE
        }
        if (longPressFired) {
            return if (listening) Action.STOP_SAVE else Action.NONE
        }
        if (listening && hitCancel) return Action.STOP_DISCARD
        if (listening && hitDone) return Action.STOP_SAVE
        if (listening) return Action.STOP_SAVE
        return Action.START
    }
}
