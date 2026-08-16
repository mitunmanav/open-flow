package app.openflow.text

import app.openflow.stt.LanguagePolicy

/**
 * Insert path: local cleanup always. LLM rewrite only when Mitun picked a brain.
 * none / on_phone stub stay local. Cleanup None (RAW) stays exact speech.
 */
object InsertPolish {
    private val RULES_BRAINS = setOf("none", "on_phone")

    fun brainRewriteOnInsert(brainId: String): Boolean {
        val id = brainId.trim().lowercase()
        return id.isNotEmpty() && id !in RULES_BRAINS
    }

    fun level(prefLevel: String): CleanupLevel = CleanupLevel.fromPref(prefLevel)

    fun language(tag: String?): String = LanguagePolicy.normalize(tag)

    fun brainIdForInsert(brainId: String): String {
        val id = brainId.trim().lowercase()
        return if (brainRewriteOnInsert(id)) id else "none"
    }
}
