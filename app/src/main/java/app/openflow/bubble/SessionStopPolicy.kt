package app.openflow.bubble

/** 5 min limit: commit text. Never discard captured whisper chunks. */
object SessionStopPolicy {
    enum class Action { COMMIT, DISCARD }

    fun onLimit(earId: String): Action {
        @Suppress("UNUSED_VARIABLE")
        val id = earId
        return Action.COMMIT
    }

    fun save(action: Action): Boolean = action == Action.COMMIT
}
