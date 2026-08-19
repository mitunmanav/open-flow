package app.openflow.bubble

object StopCommitPolicy {
    enum class Action { POLISH_INSERT, PERSIST_FAIL, DISCARD }

    fun decide(save: Boolean, raw: String): Action = when {
        !save -> Action.DISCARD
        raw.isBlank() -> Action.PERSIST_FAIL
        else -> Action.POLISH_INSERT
    }
}
