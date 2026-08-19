package app.openflow.ui.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsCatalogTest {

    @Test
    fun groups_are_speech_bubble_look_privacy_help() {
        assertThat(SettingsCatalog.groups.map { it.id })
            .containsExactly("speech", "bubble", "look", "privacy", "help")
            .inOrder()
    }

    @Test
    fun speech_is_engine_then_cleanup() {
        assertThat(SettingsCatalog.groups[0].items).containsExactly(
            SettingsItem.SpeechAi,
            SettingsItem.Cleanup,
        ).inOrder()
    }

    @Test
    fun look_includes_home_layout() {
        assertThat(SettingsCatalog.groups[2].items).containsExactly(
            SettingsItem.Appearance,
            SettingsItem.HomeLayout,
        ).inOrder()
    }

    @Test
    fun every_item_appears_once() {
        val ids = SettingsCatalog.groups.flatMap { it.items }
        assertThat(ids).containsNoDuplicates()
        assertThat(ids).containsAtLeastElementsIn(SettingsItem.entries)
        assertThat(ids).hasSize(SettingsItem.entries.size)
    }
}
