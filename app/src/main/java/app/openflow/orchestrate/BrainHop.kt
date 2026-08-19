package app.openflow.orchestrate

import app.openflow.text.InsertPolish

data class BrainHopAsk(
    val mode: RouteMode,
    val aiWhen: AiWhen,
    val brainId: String,
    val signals: RouteSignals,
    val looksLikeCommand: Boolean,
    val textLen: Int,
    val cleaned: String,
    val levelRaw: Boolean,
)

object BrainHop {
    fun miss(cleaned: String): Boolean {
        val t = cleaned.trim()
        if (t.isEmpty()) return false
        val sentences = t.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val longNoPunct = sentences.any { s ->
            s.length >= 80 && s.none { it == '.' || it == '?' || it == '!' }
        } || (sentences.size <= 1 && t.length >= 80 && t.none { it == '.' || it == '?' || it == '!' })
        val filler = Regex("(?i)\\b(um|uh|you know)\\b").containsMatchIn(t)
        val clauses = Regex("(?i)\\b(and|then|so)\\b").findAll(t).count()
        val clauseJoin = clauses >= 3 && t.none { it == '.' }
        val hits = listOf(longNoPunct, filler, clauseJoin).count { it }
        return hits >= 2
    }

    fun pick(ask: BrainHopAsk): RouteExplain {
        if (ask.looksLikeCommand) return RouteExplain("none", "command-local")
        if (ask.levelRaw) return RouteExplain("none", "raw")
        if (ask.mode == RouteMode.LOCAL_ONLY) return RouteExplain("none", "local-only")
        if (ask.textLen < 40) return RouteExplain("none", "short-skip-brain")
        val id = ask.brainId.trim().lowercase()
        if (!InsertPolish.brainRewriteOnInsert(id)) return RouteExplain("none", "fallback-none")
        if (!ask.signals.online) return RouteExplain("none", "offline")
        val keyed = ask.signals.keyedBrains.map { it.trim().lowercase() }.toSet()
        if (id !in keyed) return RouteExplain("none", "no-key")
        if (ask.mode == RouteMode.LOCAL_THEN_AI &&
            ask.aiWhen == AiWhen.MISS_ONLY &&
            !miss(ask.cleaned)
        ) {
            return RouteExplain("none", "miss-skip")
        }
        return RouteExplain(id, "user-brain")
    }
}
