package app.openflow.bubble

object BubbleTapPolicy {
    enum class Action { START, STOP_SAVE, STOP_DISCARD, NONE }

    fun action(
        listening: Boolean,
        stopInProgress: Boolean,
        dragged: Boolean,
        longPressFired: Boolean,
        hitCancel: Boolean,
        hitDone: Boolean
    ): Action {
        if (dragged || stopInProgress) return Action.NONE
        if (longPressFired) {
            return if (listening) Action.STOP_SAVE else Action.NONE
        }
        if (listening && hitCancel) return Action.STOP_DISCARD
        if (listening && hitDone) return Action.STOP_SAVE
        if (listening) return Action.STOP_SAVE
        return Action.START
    }
}
