package app.openflow.text

import app.openflow.stt.LanguagePolicy

/**
 * Insert path: keep STT + local cleanup. Never LLM-rewrite just because a brain is picked.
 */
object InsertPolish {
    fun brainRewriteOnInsert(brainId: String): Boolean {
        brainId.trim()
        return false
    }

    fun level(prefLevel: String): CleanupLevel = CleanupLevel.fromPref(prefLevel)

    fun language(tag: String?): String = LanguagePolicy.normalize(tag)

    fun brainIdForInsert(brainId: String): String {
        brainId.trim()
        return "none"
    }
}
