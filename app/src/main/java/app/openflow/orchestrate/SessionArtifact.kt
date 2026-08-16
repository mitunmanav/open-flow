package app.openflow.orchestrate

data class SessionArtifact(
    val raw: String = "",
    val cleaned: String = "",
    val ai: String = "",
) {
    fun bestAvailable(): String = when {
        ai.isNotBlank() -> ai.trim()
        cleaned.isNotBlank() -> cleaned.trim()
        else -> raw.trim()
    }
}
