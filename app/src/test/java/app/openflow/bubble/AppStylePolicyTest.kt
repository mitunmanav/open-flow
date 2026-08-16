package app.openflow.bubble

import app.openflow.prefs.FlowPrefs
import app.openflow.prefs.MemoryPrefsStore
import app.openflow.text.WritingStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppStylePolicyTest {

    @Test
    fun whatsapp_is_personal() {
        assertThat(AppStylePolicy.category("com.whatsapp")).isEqualTo("personal")
        assertThat(AppStylePolicy.styleFor("com.whatsapp", WritingStyle.FORMAL))
            .isEqualTo(WritingStyle.CASUAL)
    }

    @Test
    fun gmail_is_email() {
        assertThat(AppStylePolicy.category("com.google.android.gm")).isEqualTo("email")
        assertThat(AppStylePolicy.styleFor("com.google.android.gm", WritingStyle.CASUAL))
            .isEqualTo(WritingStyle.FORMAL)
    }

    @Test
    fun slack_is_work() {
        assertThat(AppStylePolicy.category("com.Slack")).isEqualTo("work")
        assertThat(AppStylePolicy.styleFor("com.Slack", WritingStyle.CASUAL))
            .isEqualTo(WritingStyle.FORMAL)
    }

    @Test
    fun unknown_uses_fallback() {
        assertThat(AppStylePolicy.category("app.openflow.debug")).isEqualTo("other")
        assertThat(AppStylePolicy.styleFor("app.openflow.debug", WritingStyle.EXCITED))
            .isEqualTo(WritingStyle.EXCITED)
    }

    @Test
    fun custom_prefs_override_app_style() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        prefs.setCategoryStyle(AppCategory.MESSAGING, WritingStyle.FORMAL)

        val style = AppStylePolicy.styleFor("com.whatsapp", WritingStyle.CASUAL, prefs)
        assertThat(style).isEqualTo(WritingStyle.FORMAL)
    }
}
