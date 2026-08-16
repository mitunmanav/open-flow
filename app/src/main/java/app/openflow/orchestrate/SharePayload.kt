package app.openflow.orchestrate

object SharePayload {
    /** History row share: prefer stored text (best), else raw. */
    fun forRow(text: String, rawText: String): String =
        text.trim().ifEmpty { rawText.trim() }

    fun forArtifact(a: SessionArtifact): String = a.bestAvailable()
}
