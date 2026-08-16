package app.openflow.prefs

import app.openflow.bubble.AppCategory
import app.openflow.bubble.AppOverride
import app.openflow.text.StyleCategory
import app.openflow.text.WritingStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StyleHubPrefsTest {

    @Test
    fun style_category_defaults_and_save() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.getHubStyle(StyleCategory.PERSONAL)).isEqualTo(WritingStyle.CASUAL)
        assertThat(prefs.getHubStyle(StyleCategory.EMAIL)).isEqualTo(WritingStyle.FORMAL)

        prefs.setHubStyle(StyleCategory.PERSONAL, WritingStyle.VERY_CASUAL)
        assertThat(prefs.getHubStyle(StyleCategory.PERSONAL)).isEqualTo(WritingStyle.VERY_CASUAL)
    }

    @Test
    fun disallowed_style_is_coerced_on_read() {
        val store = MemoryPrefsStore()
        store.putString("hub_style_PERSONAL", WritingStyle.EXCITED.name)
        val prefs = FlowPrefs(store)
        assertThat(prefs.getHubStyle(StyleCategory.PERSONAL)).isEqualTo(WritingStyle.CASUAL)
    }

    @Test
    fun app_assignments_crud() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.getStyleAppAssignments()).isEmpty()

        prefs.setStyleAppAssignment("com.slack", StyleCategory.WORK)
        prefs.setStyleAppAssignment("com.whatsapp", StyleCategory.PERSONAL)
        assertThat(prefs.getStyleAppAssignments()).containsExactly(
            "com.slack", StyleCategory.WORK,
            "com.whatsapp", StyleCategory.PERSONAL,
        )

        prefs.removeStyleAppAssignment("com.slack")
        assertThat(prefs.getStyleAppAssignments()).containsExactly(
            "com.whatsapp", StyleCategory.PERSONAL,
        )
    }

    @Test
    fun migrate_legacy_app_context_once() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        prefs.setCategoryStyle(AppCategory.MESSAGING, WritingStyle.VERY_CASUAL)
        prefs.setCategoryStyle(AppCategory.EMAIL, WritingStyle.CASUAL)
        prefs.saveAppOverride(
            AppOverride(
                packageName = "com.example.foo",
                category = AppCategory.WORK_COLLAB,
                style = WritingStyle.FORMAL,
                customPrompt = "ignored",
            )
        )

        prefs.migrateLegacyAppContextIfNeeded()

        assertThat(prefs.getHubStyle(StyleCategory.PERSONAL)).isEqualTo(WritingStyle.VERY_CASUAL)
        assertThat(prefs.getHubStyle(StyleCategory.EMAIL)).isEqualTo(WritingStyle.CASUAL)
        assertThat(prefs.getStyleAppAssignments()["com.example.foo"])
            .isEqualTo(StyleCategory.WORK)

        // Idempotent
        prefs.setHubStyle(StyleCategory.PERSONAL, WritingStyle.FORMAL)
        prefs.migrateLegacyAppContextIfNeeded()
        assertThat(prefs.getHubStyle(StyleCategory.PERSONAL)).isEqualTo(WritingStyle.FORMAL)
    }

    @Test
    fun hubStylesMap_covers_all_categories() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        prefs.setHubStyle(StyleCategory.WORK, WritingStyle.EXCITED)
        val map = prefs.hubStylesMap()
        assertThat(map.keys).containsExactlyElementsIn(StyleCategory.entries)
        assertThat(map[StyleCategory.WORK]).isEqualTo(WritingStyle.EXCITED)
    }
}
