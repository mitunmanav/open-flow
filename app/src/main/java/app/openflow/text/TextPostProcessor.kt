package app.openflow.text

/**
 * Facade over dictionary, snippets, and [CleanupPipeline].
 * Prefer [polishSessionResult] for production stop / debug inject path.
 *
 * **Pipeline order** (Wispr-like: expand shortcuts, then format):
 * 1. [applyDictionary] — user vocabulary (whole-word, case-insensitive)
 * 2. [expandSnippets] — exact-trigger expand
 * 3. [CleanupPipeline.run] — cleanup level + spoken cmds + [WritingStyle]/[CustomStyleConfig]
 *
 * Local rules only — no cloud AI rewrite.
 * [CleanupResult.raw] is always the original STT string (pre dict/snippet).
 */
object TextPostProcessor {

    fun polishSession(
        raw: String,
        style: WritingStyle = WritingStyle.CASUAL,
        courseCorrect: Boolean = true,
        custom: CustomStyleConfig = CustomStyleConfig(),
        dictionary: Map<String, String> = emptyMap(),
        snippets: Map<String, String> = emptyMap()
    ): String =
        polishSessionResult(
            raw = raw,
            style = style,
            level = if (courseCorrect) CleanupLevel.NORMAL else CleanupLevel.LIGHT,
            custom = custom,
            dictionary = dictionary,
            snippets = snippets
        ).clean

    fun polishSessionResult(
        raw: String,
        style: WritingStyle = WritingStyle.CASUAL,
        level: CleanupLevel = CleanupLevel.NORMAL,
        custom: CustomStyleConfig = CustomStyleConfig(),
        dictionary: Map<String, String> = emptyMap(),
        snippets: Map<String, String> = emptyMap()
    ): CleanupResult {
        val original = raw
        var t = raw
        t = applyDictionary(t, dictionary)
        t = expandSnippets(t, snippets)
        val result = CleanupPipeline.run(t, level, style, custom)
        return result.copy(raw = original.trim().ifEmpty { original })
    }

    /** @deprecated Use [WritingStyle]. Kept for binary-safe renames in prefs UI. */
    @Deprecated("Use WritingStyle")
    enum class Style {
        CASUAL, FORMAL, EXCITED;

        fun toWritingStyle(): WritingStyle = when (this) {
            FORMAL -> WritingStyle.FORMAL
            EXCITED -> WritingStyle.EXCITED
            CASUAL -> WritingStyle.CASUAL
        }
    }

    /** Unit/legacy helper — Light cleanup + style (no dict/snippet). */
    fun process(raw: String, style: WritingStyle = WritingStyle.CASUAL): String =
        CleanupPipeline.run(raw, CleanupLevel.LIGHT, style).clean

    fun applyDictionary(text: String, replacements: Map<String, String>): String {
        if (replacements.isEmpty()) return text
        var out = text
        replacements.entries.sortedByDescending { it.key.length }.forEach { (from, to) ->
            if (from.isBlank()) return@forEach
            val regex = Regex("\\b${Regex.escape(from)}\\b", RegexOption.IGNORE_CASE)
            out = regex.replace(out) { to }
        }
        return out
    }

    fun expandSnippets(text: String, snippets: Map<String, String>): String {
        if (snippets.isEmpty()) return text
        var out = text.trim()
        snippets.entries.sortedByDescending { it.key.length }.forEach { (trigger, body) ->
            if (trigger.isBlank()) return@forEach
            if (out.equals(trigger, ignoreCase = true)) {
                out = body
            }
        }
        return out
    }
}
