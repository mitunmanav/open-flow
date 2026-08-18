package app.openflow.ui.qa

import app.openflow.ui.shell.AppRoute
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * Crack-catcher: every open→close route and the tags screens must keep.
 * Reads ui source. Does not write it.
 */
class UiPathTest {

    @Test
    fun every_open_close_route_exists() {
        val required = listOf(
            AppRoute.Home,
            AppRoute.History,
            AppRoute.Dictionary,
            AppRoute.Snippets,
            AppRoute.Style,
            AppRoute.Insights,
            AppRoute.Settings,
            AppRoute.SpeechAi,
            AppRoute.Appearance,
            AppRoute.BubbleSettings,
            AppRoute.Haptics,
            AppRoute.HomeModules,
            AppRoute.Cleanup,
            AppRoute.Privacy,
            AppRoute.PrivacyPolicy,
            AppRoute.Terms,
            AppRoute.Sounds,
            AppRoute.Setup,
        )
        assertThat(AppRoute.entries.toList()).containsAtLeastElementsIn(required)
        // New route without this list = crack. Add it here on purpose.
        assertThat(AppRoute.entries).hasSize(required.size)
    }

    @Test
    fun required_screen_tags_stay_in_source() {
        val source = UiSourceScan.uiKtText()
        REQUIRED_TAGS.forEach { tag ->
            assertWithMessage("missing testTag \"$tag\" in ui/**")
                .that(UiSourceScan.hasQuotedTag(source, tag))
                .isTrue()
        }
    }

    @Test
    fun path_tags_cover_open_to_close() {
        val source = UiSourceScan.uiKtText()
        PATH_TAGS.forEach { tag ->
            assertWithMessage("path crack: missing testTag \"$tag\"")
                .that(UiSourceScan.hasQuotedTag(source, tag))
                .isTrue()
        }
    }

    @Test
    fun internet_declared_unused_until_pick() {
        val manifest = File(
            UiSourceScan.projectRoot(),
            "app/src/main/AndroidManifest.xml"
        ).readText()
        assertThat(manifest).contains("android.permission.INTERNET")
        assertThat(manifest).contains("android.intent.action.VIEW")
        assertThat(manifest).contains("android:scheme=\"https\"")
    }

    @Test
    fun privacy_no_internet_says_declared_not_absent() {
        val values = File(UiSourceScan.projectRoot(), "app/src/main/res/values")
        val xml = values.listFiles().orEmpty()
            .filter { it.extension == "xml" }
            .joinToString("\n") { it.readText() }
        assertThat(xml.lowercase()).doesNotContain("declares no internet")
        assertThat(xml.lowercase()).doesNotContain("no internet permission")
        assertThat(xml).contains("INTERNET is declared")
    }

    companion object {
        val REQUIRED_TAGS = listOf(
            "home_hub",
            "home_stats",
            "home_local_note",
            "home_note_field",
            "home_history_search",
            "bubble_preview",
            "appearance_reset",
            "appearance_color_bg",
            "haptics_reset",
            "dict_word",
            "privacy_auto_learn",
            "legal_privacy",
            "legal_terms",
        )

        val PATH_TAGS = listOf(
            "setup_wizard",
            "walkthrough",
            "walkthrough_next",
            "walkthrough_skip",
            "shell_title",
            "nav_back",
            "history_export",
            "history_export_md",
            "history_export_plain",
            "history_export_json",
            "history_export_raw",
            "history_export_save",
            "history_edit",
            "engine_settings",
            "engine_feature_chips",
            "engine_honesty",
            "engine_ear_disabled",
            "route_local_only",
            "route_local_then_ai",
            "route_ai_first",
            "ai_when_every",
            "ai_when_miss",
            "nav_settings",
            "nav_snippets",
            "dict_fab",
            "dict_auto_row",
            "snippet_fab",
            "home_stats_pages",
            "home_banner_repair",
            "home_open_insights",
            "home_legal_privacy",
            "home_legal_terms",
            "home_legal_help",
            "history_copy",
            "history_share",
            "history_more",
            "nav_insights",
            "insights_screen",
            "insights_tab_usage",
            "insights_tab_voice",
            "insights_share",
            "insights_share_text",
            "insights_share_card",
            "on_device_off",
            "on_device_on",
            "on_device_honesty",
        )
    }
}
