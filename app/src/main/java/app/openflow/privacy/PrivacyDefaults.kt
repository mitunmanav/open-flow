package app.openflow.privacy

/**
 * Privacy defaults: local-first. Online features stay off until user opts in.
 * Base APK does not declare INTERNET.
 */
data class PrivacyDefaults(
    val preferOnDeviceStt: Boolean = true,
    val allowCloudStt: Boolean = false,
    val allowSync: Boolean = false,
    val allowCrashReports: Boolean = false,
    val analyticsEnabled: Boolean = false
) {
    fun reportText(): String = buildString {
        appendLine("Open Flow privacy report")
        appendLine("- prefer on-device STT: ${onOff(preferOnDeviceStt)}")
        appendLine("- cloud STT: ${onOff(allowCloudStt)}")
        appendLine("- sync: ${onOff(allowSync)}")
        appendLine("- crash reports: ${onOff(allowCrashReports)}")
        appendLine("- analytics: ${onOff(analyticsEnabled)}")
        appendLine("- INTERNET permission: not declared by default")
        appendLine("- account required: no")
    }

    private fun onOff(v: Boolean) = if (v) "ON" else "OFF"
}
