package app.openflow.text

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import kotlinx.coroutines.runBlocking

/**
 * Facade over dictionary, snippets, and [CleanupPipeline].
 * Prefer [polishSessionResult] for production stop / debug inject path.
 *
 * **Pipeline order** (nothing falls through):
 * 1. [applyDictionary] — user vocabulary (whole-word, case-insensitive)
 * 2. [expandSnippets] — whole-word/phrase expand ([PhraseMap] is spoken cmds, not snippets)
 * 3. [CleanupPipeline.run]:
 *    - Light: normalize → fillers → reps → [VoiceCommands] → lightGrammar
 *    - Medium: + false starts → [CourseCorrector] → lists → lightClarity
 *    - High: + hedges
 *    - then [StyleApplicator] / [SentenceFormat] / [WritingStyle]
 * 4. High + [FeatureAuto] HIGH_AI (or [brainRewrite]) → injected [TextAIProvider.enhance]
 *    after rules. Default [NoAI].
 *
 * Empty in → empty out. Non-empty content must not vanish (except explicit clear).
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
        snippets: Map<String, String> = emptyMap(),
        brain: TextAIProvider = NoAI,
        brainRewrite: Boolean = false,
        earId: String = "system",
        brainId: String = "none",
        languages: Set<String> = emptySet(),
    ): CleanupResult {
        val original = raw
        var t = raw
        t = applyDictionary(
            t,
            dictionary,
            sides = LearnEngine.sideBags(),
            autoKeys = LearnEngine.autoKeys()
        )
        t = expandSnippets(t, snippets)
        val result = CleanupPipeline.run(t, level, style, custom)
        val highAi = brainRewrite ||
            Feature.HIGH_AI in FeatureAuto.of(earId, brainId, languages)
        val cleaned = if (level == CleanupLevel.HIGH && highAi) {
            runBlocking { brain.enhance(result.clean, "cleanup") }
        } else {
            result.clean
        }
        return result.copy(raw = original.trim().ifEmpty { original }, clean = cleaned)
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

    fun applyDictionary(
        text: String,
        replacements: Map<String, String>,
        sides: Map<String, Set<String>> = emptyMap(),
        autoKeys: Set<String> = emptySet()
    ): String = LearnEngine.applyPairs(text, replacements, sides, autoKeys)

    fun expandSnippets(text: String, snippets: Map<String, String>): String {
        if (snippets.isEmpty() || text.isEmpty()) return text
        var out = text
        snippets.entries
            .filter { it.key.isNotBlank() }
            .sortedByDescending { it.key.length }
            .forEach { (trigger, body) ->
                val needle = trigger.trim()
                if (needle.isEmpty()) return@forEach
                val regex = Regex(
                    "(?<![\\p{L}\\p{N}])${Regex.escape(needle)}(?![\\p{L}\\p{N}])",
                    RegexOption.IGNORE_CASE
                )
                out = regex.replace(out) { body }
            }
        return out
    }
}
