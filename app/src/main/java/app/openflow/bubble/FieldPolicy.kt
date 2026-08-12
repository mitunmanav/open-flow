package app.openflow.bubble

/**
 * Pure rules: which focused fields accept dictation insert.
 * Mirrors Wispr-style skip of password / phone / sensitive inputs.
 */
object FieldPolicy {

    fun isSensitive(
        isPassword: Boolean,
        inputType: Int,
        className: String?,
        hintOrDesc: String?
    ): Boolean {
        if (isPassword) return true
        if (isPhoneOrNumberInputType(inputType)) return true
        val hay = listOfNotNull(className, hintOrDesc).joinToString(" ").lowercase()
        if (hay.contains("password") || hay.contains("pin") || hay.contains("otp")) return true
        if (hay.contains("phone") && hay.contains("edit")) return true
        return false
    }

    fun isEditableClass(className: String?): Boolean {
        if (className.isNullOrBlank()) return false
        val c = className.lowercase()
        return c.contains("edittext") ||
            c.contains("textfield") ||
            c.contains("autocompletetextview") ||
            c.contains("textinputedittext") ||
            c.contains("webView".lowercase()) // WebView may host inputs; still try insert
    }

    /** Append spoken text to existing field content. */
    fun mergeInsert(existing: CharSequence?, spoken: String): String {
        val base = existing?.toString().orEmpty()
        val piece = spoken.trim()
        if (piece.isEmpty()) return base
        if (base.isEmpty()) return piece
        val needsSpace = !base.last().isWhitespace() &&
            !piece.first().isWhitespace() &&
            piece.first() !in ".,!?;:\n"
        return if (needsSpace) "$base $piece" else base + piece
    }

    /**
     * Wispr-style session write: prefix (text already in field before listen)
     * + one polished session blob. Never stacks every STT final as a dump.
     */
    fun mergeSession(prefix: CharSequence?, sessionText: String): String {
        val base = prefix?.toString().orEmpty()
        val piece = sessionText.trim()
        if (piece.isEmpty()) return base
        if (base.isEmpty()) return piece
        val head = base.trim()
        if (head.isNotEmpty() && piece.startsWith(head, ignoreCase = true)) return piece
        val needsSpace = !base.last().isWhitespace() &&
            !piece.first().isWhitespace() &&
            piece.first() !in ".,!?;:\n"
        return if (needsSpace) "$base $piece" else base + piece
    }

    private fun isPhoneOrNumberInputType(inputType: Int): Boolean {
        // android.text.InputType flags (avoid Android dependency in pure unit tests by numeric values)
        val TYPE_MASK_CLASS = 0x0000000f
        val TYPE_CLASS_PHONE = 0x00000003
        val TYPE_CLASS_NUMBER = 0x00000002
        val cls = inputType and TYPE_MASK_CLASS
        return cls == TYPE_CLASS_PHONE || cls == TYPE_CLASS_NUMBER
    }
}
