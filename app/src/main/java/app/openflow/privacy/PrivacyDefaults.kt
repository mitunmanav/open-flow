package app.openflow.privacy

import app.openflow.prefs.FlowPrefs

/**
 * Static privacy report. Honest about Android system STT
 * (may stream audio remotely depending on device / OEM).
 * OpenFlow itself has no server, analytics, or uploads.
 */
object PrivacyDefaults {
    fun reportText(prefs: FlowPrefs? = null): String {
        val retention = prefs?.retentionPolicy ?: "keep"
        val historyLocation = when (retention) {
            "never_store" -> "not stored in Room history"
            "wipe_24h" -> "on device, purged after 24h"
            else -> "on device SQLite (not encrypted)"
        }

        return """
            Open Flow privacy report
            - Speech recognition: Android system STT (may leave device)
            - OpenFlow server: none
            - Analytics: disabled
            - Audio uploaded by OpenFlow: never
            - Transcript uploaded by OpenFlow: never
            - Local history: $historyLocation ($retention)
            - prefer offline STT extras: ON (device may still use remote STT)
            - sync: OFF
            - crash reports: OFF
            - INTERNET permission: declared; unused until user pick
            - account required: no
        """.trimIndent() + "\n"
    }
}
