package app.openflow.ui.settings

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class SettingsOrgScanTest {
    private fun ui(path: String) =
        File(UiSourceScan.projectRoot(), "app/src/main/java/app/openflow/ui/$path").readText()

    @Test
    fun hub_renders_catalog_not_a_flat_pile() {
        val hub = ui("settings/SettingsHub.kt")
        assertThat(hub).contains("SettingsCatalog.groups")
        assertThat(hub).contains("settings_hub")
        assertThat(hub).contains("onOpen")
        assertThat(hub).doesNotContain("onSpeechAi")
    }

    @Test
    fun appearance_has_no_stt_controls() {
        val src = ui("settings/AppearanceSettings.kt")
        assertThat(src).doesNotContain("Dictation speed")
        assertThat(src).doesNotContain("Speech language")
        assertThat(src).doesNotContain("on_device_off")
        assertThat(src).doesNotContain("preferOnDevice")
    }

    @Test
    fun appearance_is_theme_and_refresh_only() {
        val src = ui("settings/AppearanceSettings.kt")
        assertThat(src).doesNotContain("AppearanceColorRow")
        assertThat(src).doesNotContain("appearance_color_bg")
        assertThat(src).doesNotContain("Reset colors")
        assertThat(src).doesNotContain("colorBg")
        assertThat(src).contains("Color theme")
        assertThat(src).contains("Screen refresh")
        assertThat(src).contains("appearance_theme")
        assertThat(src).contains("SettingsPage")
    }

    @Test
    fun peeled_subpages_do_not_import_engine_screen() {
        listOf(
            "AppearanceSettings.kt",
            "BubbleSettings.kt",
            "CleanupSettings.kt",
            "ModuleEditor.kt",
            "PrivacySettings.kt",
            "SettingsRow.kt",
            "SoundsSettings.kt",
        ).forEach { name ->
            assertThat(ui("settings/$name")).doesNotContain(
                "import app.openflow.ui.engine.EngineSettingsScreen",
            )
        }
    }

    @Test
    fun engine_uses_settings_page_and_section_titles() {
        val src = ui("engine/EngineSettingsScreen.kt")
        assertThat(src).contains("SettingsPage")
        assertThat(src).contains("SettingsSectionTitle")
    }

    @Test
    fun speech_device_settings_holds_stt_controls() {
        val src = ui("settings/SpeechDeviceSettings.kt")
        assertThat(src).contains("on_device_off")
        assertThat(src).contains("on_device_on")
        assertThat(src).contains("on_device_honesty")
        assertThat(src).contains("Dictation speed")
        assertThat(src).contains("Speech language")
    }

    @Test
    fun engine_screen_includes_device_block() {
        val src = ui("engine/EngineSettingsScreen.kt")
        assertThat(src).contains("SpeechDeviceSettings")
    }

    @Test
    fun settings_page_uses_scheme_tokens_not_cream() {
        val src = ui("settings/SettingsPage.kt")
        assertThat(src).doesNotContain("SecUi.cream")
        assertThat(src).doesNotContain("SecUi.muted")
        assertThat(src).contains("colorScheme.background")
        assertThat(src).contains("onSurfaceVariant")
    }

    @Test
    fun privacy_page_has_no_legal_buttons() {
        val src = ui("settings/PrivacySettings.kt")
        assertThat(src).doesNotContain("onPrivacyPolicy")
        assertThat(src).doesNotContain("onTerms")
        assertThat(src).doesNotContain("Privacy policy")
        assertThat(src).doesNotContain("Terms of use")
    }
}
