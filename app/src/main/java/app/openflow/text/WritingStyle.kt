package app.openflow.text

/**
 * Writing tone applied *after* cleanup levels (Wispr Style, local rules only).
 *
 * Built-ins:
 * - FORMAL — email/docs: sentence case, always end ., expand informal (gonna→going to)
 * - CASUAL — everyday: sentence case, period when long
 * - VERY_CASUAL — chat: soft caps, no forced period
 * - EXCITED — energy: prefer !
 * - CUSTOM — user end-punct / caps / informal expand / replace lines
 *
 * Not LLM tone rewrite. Presentation + user replace rules only.
 */
enum class WritingStyle {
    FORMAL,
    CASUAL,
    VERY_CASUAL,
    EXCITED,
    CUSTOM;

    companion object {
        fun fromPref(value: String?): WritingStyle {
            val v = value?.trim()?.uppercase()?.replace('-', '_') ?: return CASUAL
            return when (v) {
                "FORMAL" -> FORMAL
                "VERY_CASUAL", "VERYCASUAL", "VERY CASUAL" -> VERY_CASUAL
                "EXCITED" -> EXCITED
                "CUSTOM" -> CUSTOM
                "CASUAL" -> CASUAL
                // legacy TextPostProcessor.Style names
                else -> runCatching { valueOf(v) }.getOrDefault(CASUAL)
            }
        }
    }
}

enum class EndPunct {
    /** Style default (formal=., casual=long only, very_casual=none, excited=!). */
    AUTO,
    PERIOD,
    BANG,
    NONE;

    companion object {
        fun fromPref(value: String?): EndPunct = when (value?.lowercase()) {
            "period", "." -> PERIOD
            "bang", "!", "exclamation" -> BANG
            "none" -> NONE
            else -> AUTO
        }
    }
}

enum class CapsMode {
    /** Capitalize starts of sentences. */
    SENTENCE,
    /** Only first character of the whole blob. */
    FIRST,
    /** Leave casing from STT/cleanup (still fixes lone "i" → "I"). */
    NONE;

    companion object {
        fun fromPref(value: String?): CapsMode = when (value?.lowercase()) {
            "first" -> FIRST
            "none" -> NONE
            else -> SENTENCE
        }
    }
}

/**
 * User custom style knobs (stored in prefs as plain strings).
 * [replacements] are case-insensitive whole-word pairs, user-authored.
 */
data class CustomStyleConfig(
    val endPunct: EndPunct = EndPunct.AUTO,
    val caps: CapsMode = CapsMode.SENTENCE,
    val expandInformal: Boolean = false,
    val replacements: List<Pair<String, String>> = emptyList()
) {
    companion object {
        /** Lines: `from=>to` or `from=to`. Empty lines / # comments ignored. */
        fun parseReplacements(blob: String): List<Pair<String, String>> {
            if (blob.isBlank()) return emptyList()
            return blob.lineSequence().mapNotNull { line ->
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("#")) return@mapNotNull null
                val sep = when {
                    t.contains("=>") -> "=>"
                    t.contains("=") -> "="
                    else -> return@mapNotNull null
                }
                val parts = t.split(sep, limit = 2)
                val from = parts.getOrNull(0)?.trim().orEmpty()
                val to = parts.getOrNull(1)?.trim().orEmpty()
                if (from.isEmpty()) null else from to to
            }.toList()
        }
    }
}

object StyleApplicator {

    private val informal = listOf(
        "gonna" to "going to",
        "wanna" to "want to",
        "gotta" to "got to",
        "kinda" to "kind of",
        "sorta" to "sort of",
        "dunno" to "do not know",
        "ain't" to "is not",
        "can't" to "cannot",
        "won't" to "will not",
        "don't" to "do not",
        "doesn't" to "does not",
        "isn't" to "is not",
        "aren't" to "are not",
        "wasn't" to "was not",
        "weren't" to "were not",
        "i'm" to "I am",
        "it's" to "it is",
        "that's" to "that is",
        "there's" to "there is",
        "we're" to "we are",
        "they're" to "they are",
        "you're" to "you are",
        "i've" to "I have",
        "we've" to "we have",
        "they've" to "they have",
        "i'll" to "I will",
        "we'll" to "we will",
        "they'll" to "they will",
        "i'd" to "I would"
    )
    private val informalRegexes: List<Pair<Regex, String>> =
        informal.sortedByDescending { it.first.length }.map { (from, to) ->
            Regex("\\b${Regex.escape(from)}\\b", RegexOption.IGNORE_CASE) to to
        }

    fun apply(
        text: String,
        style: WritingStyle,
        custom: CustomStyleConfig = CustomStyleConfig()
    ): String {
        if (text.isBlank()) return text
        var t = text.trim()

        // User replacements first (custom style owns them; also available for formal polish path)
        val pairs = when (style) {
            WritingStyle.CUSTOM -> custom.replacements
            else -> emptyList()
        }
        pairs.sortedByDescending { it.first.length }.forEach { (from, to) ->
            t = t.replace(
                Regex("\\b${Regex.escape(from)}\\b", RegexOption.IGNORE_CASE),
                to
            )
        }

        val expand = when (style) {
            WritingStyle.FORMAL -> true
            WritingStyle.CUSTOM -> custom.expandInformal
            else -> false
        }
        if (expand) {
            informalRegexes.forEach { (re, to) ->
                t = re.replace(t, to)
            }
        }

        t = fixLoneI(t)

        val caps = when (style) {
            WritingStyle.FORMAL, WritingStyle.CASUAL, WritingStyle.EXCITED -> CapsMode.SENTENCE
            WritingStyle.VERY_CASUAL -> CapsMode.FIRST
            WritingStyle.CUSTOM -> custom.caps
        }
        t = applyCaps(t, caps)

        val endMode = when (style) {
            WritingStyle.FORMAL -> EndPunct.PERIOD
            WritingStyle.EXCITED -> EndPunct.BANG
            WritingStyle.VERY_CASUAL -> EndPunct.NONE
            WritingStyle.CASUAL -> EndPunct.AUTO
            WritingStyle.CUSTOM -> custom.endPunct
        }
        t = applyEndPunct(t, endMode, style)

        // Collapse horizontal space only — keep newlines (lists / new paragraph).
        t = t.lines().joinToString("\n") { line ->
            line.replace(Regex("[ \\t]+"), " ").trim()
        }.replace(Regex("\n{3,}"), "\n\n").trim()
        t = t.replace(Regex("""[ \\t]+([.,!?;:])"""), "$1")
        return t
    }

    private fun fixLoneI(t: String): String =
        t.replace(Regex("""\bi\b"""), "I")

    private fun applyCaps(t: String, mode: CapsMode): String {
        if (t.isEmpty()) return t
        return when (mode) {
            CapsMode.NONE -> t
            CapsMode.FIRST -> t.replaceFirstChar { it.uppercaseChar() }
            CapsMode.SENTENCE -> SentenceFormat.capitalizeSentences(t)
        }
    }

    private fun applyEndPunct(t: String, mode: EndPunct, style: WritingStyle): String {
        var s = QuestionPolicy.applyAll(t.trimEnd())
        if (s.isEmpty()) return s
        if (s.trimEnd().endsWith('?')) return s

        val last = s.lastOrNull()
        val hasEnd = last == '.' || last == '!' || last == '?'

        // Built-in formal always ends with period (not !); excited prefers !
        if (hasEnd) {
            return when {
                style == WritingStyle.EXCITED && (last == '.' || last == '!') ->
                    s.dropLast(1) + "!"
                style == WritingStyle.FORMAL && (last == '.' || last == '!') ->
                    s.dropLast(1) + "."
                mode == EndPunct.PERIOD && last == '!' -> s.dropLast(1) + "."
                mode == EndPunct.BANG && last == '.' -> s.dropLast(1) + "!"
                mode == EndPunct.NONE && (last == '.' || last == '!') -> s.dropLast(1)
                else -> s
            }
        }

        if (!s.last().isLetterOrDigit()) return s

        return when (mode) {
            EndPunct.PERIOD -> "$s."
            EndPunct.BANG -> "$s!"
            EndPunct.NONE -> s
            EndPunct.AUTO -> when {
                style == WritingStyle.EXCITED -> "$s!"
                style == WritingStyle.FORMAL -> "$s."
                // Casual: period only when the line is long; very_casual never reaches AUTO
                s.length > 40 -> "$s."
                else -> s
            }
        }
    }
}
