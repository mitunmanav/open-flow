package app.openflow.prefs

import app.openflow.bubble.AppCategory
import app.openflow.bubble.AppOverride
import app.openflow.text.WritingStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlowPrefsAppContextTest {

    @Test
    fun default_app_context_enabled_is_true() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.appContextEnabled).isTrue()
        prefs.appContextEnabled = false
        assertThat(prefs.appContextEnabled).isFalse()
    }

    @Test
    fun category_styles_can_be_customized_and_persisted() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.getCategoryStyle(AppCategory.MESSAGING)).isEqualTo(WritingStyle.CASUAL)
        assertThat(prefs.getCategoryStyle(AppCategory.EMAIL)).isEqualTo(WritingStyle.FORMAL)

        prefs.setCategoryStyle(AppCategory.MESSAGING, WritingStyle.FORMAL)
        assertThat(prefs.getCategoryStyle(AppCategory.MESSAGING)).isEqualTo(WritingStyle.FORMAL)
    }

    @Test
    fun category_custom_prompts_can_be_saved() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.getCategoryPrompt(AppCategory.WORK_COLLAB)).isEmpty()

        prefs.setCategoryPrompt(AppCategory.WORK_COLLAB, "Use bullet points and keep concise")
        assertThat(prefs.getCategoryPrompt(AppCategory.WORK_COLLAB)).isEqualTo("Use bullet points and keep concise")
    }

    @Test
    fun app_overrides_crud_operations() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.getAppOverrides()).isEmpty()

        val override = AppOverride(
            packageName = "com.slack",
            category = AppCategory.WORK_COLLAB,
            style = WritingStyle.CASUAL,
            customPrompt = "Slack team updates style"
        )
        prefs.saveAppOverride(override)

        val overrides = prefs.getAppOverrides()
        assertThat(overrides).hasSize(1)
        assertThat(overrides[0].packageName).isEqualTo("com.slack")
        assertThat(overrides[0].style).isEqualTo(WritingStyle.CASUAL)
        assertThat(overrides[0].customPrompt).isEqualTo("Slack team updates style")

        assertThat(prefs.getAppOverride("com.slack")).isNotNull()
        assertThat(prefs.getAppOverride("com.slack")?.customPrompt).isEqualTo("Slack team updates style")

        prefs.deleteAppOverride("com.slack")
        assertThat(prefs.getAppOverrides()).isEmpty()
        assertThat(prefs.getAppOverride("com.slack")).isNull()
    }
}
