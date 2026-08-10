package app.openflow.privacy

/** Local-first privacy report. Flags not toggled yet — hard defaults. */
object PrivacyDefaults {
    fun reportText(): String = """
        Open Flow privacy report
        - prefer on-device STT: ON
        - cloud STT: OFF
        - sync: OFF
        - crash reports: OFF
        - analytics: OFF
        - INTERNET permission: not declared by default
        - account required: no
    """.trimIndent() + "\n"
}
