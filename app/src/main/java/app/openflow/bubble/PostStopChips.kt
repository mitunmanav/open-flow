package app.openflow.bubble

/** Copy / Undo / Paste chips after stop. Auto-dismiss ~10s. */
object PostStopChips {
    const val DISMISS_MS = 10_000L

    data class State(
        val copy: Boolean,
        val undo: Boolean,
        val paste: Boolean,
    ) {
        val any: Boolean get() = copy || undo || paste
    }

    fun visible(elapsedMs: Long): Boolean = elapsedMs in 0L until DISMISS_MS

    fun state(
        elapsedMs: Long,
        hasSessionText: Boolean,
        insertOk: Boolean,
        canUndo: Boolean,
    ): State {
        if (!visible(elapsedMs) || !hasSessionText) {
            return State(copy = false, undo = false, paste = false)
        }
        return State(
            copy = false,
            undo = false,
            paste = !insertOk,
        )
    }
}
