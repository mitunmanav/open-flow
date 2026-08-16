package app.openflow.data

/** Dictation processing outcome. */
object ProcessStatus {
    const val OK = "ok"
    const val FAILED = "failed"

    fun normalize(value: String?): String = when (value?.lowercase()) {
        FAILED -> FAILED
        else -> OK
    }

    fun isFailed(value: String?): Boolean = normalize(value) == FAILED
}
