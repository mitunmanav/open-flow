package app.openflow.bubble

import app.openflow.ai.TextAIProvider
import app.openflow.text.CourseCorrector

/**
 * Surrounding field text for local polish (continue + course).
 * Always on for insert. [wrapBrain] stays opt-in (LLM). Never for analytics.
 */
object FieldContext {

    @Suppress("UNUSED_PARAMETER")
    fun on(brainRewrite: Boolean): Boolean = true

    fun surrounding(on: Boolean, fieldText: String): String =
        if (!on) "" else fieldText.trim()

    fun continueSpoken(
        field: String,
        spoken: String,
        keepCap: Set<String> = emptySet(),
    ): String {
        val f = field.trimEnd()
        val s = spoken.trim()
        if (f.isEmpty() || s.isEmpty()) return s
        val end = f.last()
        if (end in ".!?\n") return s
        val first = s.split(Regex("\\s+")).first()
        if (keepCap.any { it.equals(first, ignoreCase = true) }) return s
        return s.replaceFirstChar { ch -> ch.lowercaseChar() }
    }

    /** Full field after local continue + course-correct across the prefix boundary. */
    fun afterPolish(
        prefix: String,
        polishedSpoken: String,
        keepCap: Set<String> = emptySet(),
    ): String {
        val said = continueSpoken(prefix, polishedSpoken, keepCap)
        if (said.isEmpty()) return prefix
        if (prefix.isBlank()) return said
        val combined = FieldPolicy.mergeSession(prefix, said)
        return CourseCorrector.apply(combined)
    }

    fun enhanceInput(spoken: String, surrounding: String): String {
        val said = spoken.trim()
        val around = surrounding.trim()
        if (around.isEmpty()) return said
        return "Field: $around\nSaid: $said"
    }

    fun wrapBrain(brain: TextAIProvider, surrounding: String): TextAIProvider {
        val around = surrounding.trim()
        if (around.isEmpty()) return brain
        return object : TextAIProvider {
            override val name: String = brain.name
            override val capability = brain.capability
            override suspend fun enhance(text: String, mode: String): String =
                brain.enhance(enhanceInput(text, around), mode)
        }
    }
}
