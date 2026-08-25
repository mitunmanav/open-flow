package app.openflow.bubble

/** Cycles the dictation language from the idle-bubble badge. */
object LanguageCyclePolicy {
    fun badge(tag: String?): String {
        val t = tag?.trim().orEmpty()
        if (t.isEmpty()) return "EN"
        return t.substringBefore('-').uppercase().ifBlank { "EN" }
    }

    fun next(current: String?, supported: List<String>): String {
        if (supported.isEmpty()) return current ?: "en-US"
        val idx = supported.indexOfFirst { it.equals(current?.trim(), ignoreCase = true) }
        return supported[(idx + 1).mod(supported.size)]
    }
}
