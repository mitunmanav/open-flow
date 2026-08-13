package app.openflow.privacy

import app.openflow.prefs.FlowPrefs

/**
 * Static privacy report. Honest about Android system STT
 * (may stream audio remotely depending on device / OEM).
 * OpenFlow itself has no server, analytics, or uploads.
 */
object PrivacyDefaults {
    fun reportText(prefs: FlowPrefs? = null): String =
        reportText(prefs?.retentionPolicy ?: RetentionPolicy.KEEP)

    fun reportText(retention: String): String {
        val historyLocation = when (retention) {
            RetentionPolicy.NEVER_STORE -> "not stored in Room history"
            RetentionPolicy.WIPE_24H -> "on device, purged after 24h"
            else -> "on device SQLite (not encrypted)"
        }

        return """
            Open Flow privacy report
            - Speech recognition: Android system STT (may leave device)
            - OpenFlow server: none
            - Analytics: disabled
            - Audio uploaded by OpenFlow: only if cloud ear
            - Transcript uploaded by OpenFlow: never
            - Local history: $historyLocation ($retention)
            - prefer offline STT extras: ON (device may still use remote STT)
            - sync: OFF
            - crash reports: OFF
            - INTERNET permission: declared; unused until user pick
            - Grok: xAI (not Groq)
            - account required: no
        """.trimIndent() + "\n"
    }

    /** OpenFlow sends audio only for a cloud / laptop ear. System STT is OEM. */
    fun audioLeaves(earId: String): Boolean = when (earId.lowercase()) {
        "openai", "deepgram", "assemblyai", "sarvam", "laptop", "custom_stt" -> true
        else -> false
    }
}
