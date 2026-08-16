package app.openflow.text

/**
 * Wispr desktop "Press Enter" analog: trailing phrase only, then IME submit.
 * Mid-sentence "press enter" stays spoken text.
 */
object PressEnterPolicy {
    data class Result(val text: String, val submit: Boolean)

    private val TRAILING = Regex("""(?i)\s+press\s+enter[.!?]*\s*$""")

    fun apply(text: String): Result {
        val match = TRAILING.find(text) ?: return Result(text, submit = false)
        return Result(text.substring(0, match.range.first).trimEnd(), submit = true)
    }
}
