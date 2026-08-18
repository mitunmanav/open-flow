package app.openflow.orchestrate

import app.openflow.text.InsertPolish

enum class RouteMode {
    LOCAL_ONLY,
    LOCAL_THEN_AI,
    AI_FIRST,
    ;

    val pref: String
        get() = when (this) {
            LOCAL_ONLY -> "local_only"
            LOCAL_THEN_AI -> "local_then_ai"
            AI_FIRST -> "ai_first"
        }

    companion object {
        fun fromPref(raw: String): RouteMode =
            when (raw.trim().lowercase()) {
                "local_only" -> LOCAL_ONLY
                "ai_first" -> AI_FIRST
                else -> LOCAL_THEN_AI
            }

        fun fromLegacy(autoRoute: Boolean, brainId: String): RouteMode {
            if (autoRoute) return LOCAL_THEN_AI
            return if (InsertPolish.brainRewriteOnInsert(brainId)) LOCAL_THEN_AI else LOCAL_ONLY
        }
    }
}
