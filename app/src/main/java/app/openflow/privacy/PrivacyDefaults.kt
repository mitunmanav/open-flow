package app.openflow.privacy

/**
 * Static privacy report. Honest about Android system STT
 * (may stream audio remotely depending on device / OEM).
 * OpenFlow itself has no server, analytics, or uploads.
 */
object PrivacyDefaults {
    fun reportText(prefs: FlowPrefs? = null): String {
        val retention = prefs?.retentionPolicy ?: "keep"
        val historyLocation = if (retention == "never_store") "not stored" else "on device"
        
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
            - INTERNET permission: not declared by OpenFlow
            - account required: no
        """.trimIndent() + "\n"
    }
}
