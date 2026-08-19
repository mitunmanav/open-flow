package app.openflow.ui.settings

enum class SettingsItem(val title: String, val subtitle: String) {
    SpeechAi("Speech + AI", "Pick speech and rewrite. Where audio and text go."),
    Cleanup("Cleanup", "Filler words, course corrections, lists"),
    Bubble("Flow Bubble", "Size, look, and feel of the floating bubble"),
    Haptics("Haptics", "Off, Light, Full, or Custom feel"),
    Sounds("Sounds", "Start / stop audio cues"),
    Appearance("Appearance", "Dark / light theme and display refresh"),
    HomeLayout("Home layout", "Show, hide, reorder Home cards"),
    Privacy("Privacy & keep", "Zero-cloud audit, auto-wipe"),
    PrivacyPolicy("Privacy policy", "What this app uses. What can leave this phone."),
    Terms("Terms of use", "MIT license, permissions, no warranty"),
    Feedback("Share feedback", "Ideas and questions on GitHub Discussions"),
    ReportIssue("Report an issue", "Bugs on GitHub Issues"),
    ReportSecurity("Report a vulnerability", "Private Security Advisories only — not a public issue"),
}

data class SettingsGroup(
    val id: String,
    val title: String,
    val items: List<SettingsItem>,
)

object SettingsCatalog {
    val groups: List<SettingsGroup> = listOf(
        SettingsGroup("speech", "Speech", listOf(SettingsItem.SpeechAi, SettingsItem.Cleanup)),
        SettingsGroup(
            "bubble",
            "Bubble",
            listOf(SettingsItem.Bubble, SettingsItem.Haptics, SettingsItem.Sounds),
        ),
        SettingsGroup(
            "look",
            "Look",
            listOf(SettingsItem.Appearance, SettingsItem.HomeLayout),
        ),
        SettingsGroup(
            "privacy",
            "Privacy",
            listOf(SettingsItem.Privacy, SettingsItem.PrivacyPolicy, SettingsItem.Terms),
        ),
        SettingsGroup(
            "help",
            "Help",
            listOf(SettingsItem.Feedback, SettingsItem.ReportIssue, SettingsItem.ReportSecurity),
        ),
    )
}
