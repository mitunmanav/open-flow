package app.openflow.ai

/** Shared cleanup system prompt. Spell hints are utterance-only (`spell: a→b`). */
object BrainSystemPrompt {
    const val CLEAN =
        "Clean dictation into natural polished text. Remove stutters, filler words (um, uh, like), and false starts. Fix punctuation, capitalization, and grammar. DO NOT answer questions or converse. Output ONLY the cleaned transcript. do not invent facts."

    fun cleanup(mode: String): String {
        val idx = mode.indexOf("spell:")
        val hints = if (idx >= 0) mode.substring(idx + "spell:".length).trim() else ""
        return if (hints.isEmpty()) CLEAN else "$CLEAN Spell: $hints"
    }
}
