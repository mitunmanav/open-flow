package app.openflow.bubble

import app.openflow.text.ContinuationPolicy

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
        if (isPasswordInputVariation(inputType)) return true
        val hay = listOfNotNull(className, hintOrDesc).joinToString(" ").lowercase()
        if (hay.contains("password")) return true
        if (hasWord(hay, "pin") || hasWord(hay, "otp")) return true
        if (hasWord(hay, "phone") || hasWord(hay, "telephone")) return true
        if (hasSensitiveToken(hay)) return true
        return false
    }

    private fun hasWord(hay: String, word: String): Boolean =
        Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(hay)

    private fun hasSensitiveToken(hay: String): Boolean {
        // Word-ish tokens — avoid "accident" matching "cid".
        val tokens = listOf(
            "cvv", "cvc", "cid", "ssn", "social security", "credit card", "card number"
        )
        return tokens.any { token ->
            if (token.contains(' ')) {
                hay.contains(token)
            } else {
                Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(hay)
            }
        }
    }

    /** Hint + contentDescription only. Never live field body — "pin" in a message would skip. */
    fun skipHints(hintText: String?, contentDescription: String?): String? {
        val s = listOfNotNull(
            hintText?.takeIf { it.isNotBlank() },
            contentDescription?.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        return s.ifBlank { null }
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

    /**
     * Usable dictation target. Uses [inputType] + hint/desc only.
     * [bodyText] ignored — chat "pin" must not skip.
     */
    @Suppress("UNUSED_PARAMETER")
    fun acceptsDictation(
        enabled: Boolean,
        isEditable: Boolean,
        isPassword: Boolean,
        inputType: Int,
        className: String?,
        hintText: String?,
        contentDescription: String?,
        bodyText: String? = null
    ): Boolean {
        if (!enabled) return false
        val looksEdit = isEditable || isEditableClass(className)
        if (!looksEdit) return false
        if (isSensitive(
                isPassword = isPassword,
                inputType = inputType,
                className = className,
                hintOrDesc = skipHints(hintText, contentDescription)
            )
        ) {
            return false
        }
        return true
    }

    fun isSearch(inputType: Int, className: String?, hintOrDesc: String?): Boolean {
        val hay = listOfNotNull(className, hintOrDesc).joinToString(" ").lowercase()
        if (hay.contains("search") || hay.contains("query") || hay.contains("url bar")) return true
        // FILTER = 0xb0 (search-like). WEB_EDIT_TEXT = 0xa0 is a normal web field.
        val variation = inputType and 0x00000ff0
        return variation == 0x000000b0
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
    fun mergeSession(prefix: CharSequence?, sessionText: String): String =
        ContinuationPolicy.join(prefix?.toString().orEmpty(), sessionText)

    private fun isPhoneOrNumberInputType(inputType: Int): Boolean {
        // android.text.InputType flags (avoid Android dependency in pure unit tests by numeric values)
        val TYPE_MASK_CLASS = 0x0000000f
        val TYPE_CLASS_PHONE = 0x00000003
        val TYPE_CLASS_NUMBER = 0x00000002
        val cls = inputType and TYPE_MASK_CLASS
        return cls == TYPE_CLASS_PHONE || cls == TYPE_CLASS_NUMBER
    }

    private fun isPasswordInputVariation(inputType: Int): Boolean {
        val TYPE_MASK_VARIATION = 0x00000ff0
        val TYPE_TEXT_VARIATION_PASSWORD = 0x00000080
        val TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090
        val TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0
        val variation = inputType and TYPE_MASK_VARIATION
        return variation == TYPE_TEXT_VARIATION_PASSWORD ||
            variation == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == TYPE_TEXT_VARIATION_WEB_PASSWORD
    }
}
