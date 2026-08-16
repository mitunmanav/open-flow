package app.openflow.orchestrate

object PipelineArtifactPolicy {
    suspend fun build(
        raw: String,
        cleaned: String,
        enhance: suspend (String) -> String,
    ): SessionArtifact {
        val ai = runCatching { enhance(cleaned) }
            .getOrNull()
            ?.trim()
            ?.takeIf { candidate -> validAi(candidate, cleaned) }
            .orEmpty()
        return SessionArtifact(raw = raw, cleaned = cleaned, ai = ai)
    }

    private fun validAi(candidate: String, cleaned: String): Boolean {
        if (candidate.isBlank()) return false
        if (cleaned.isNotBlank() && candidate.length > cleaned.length * 4 + 100) return false
        return true
    }
}
