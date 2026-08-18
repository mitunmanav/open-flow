package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeBannerPolicyTest {
    @Test
    fun repair_wins() {
        assertThat(
            HomeBannerPolicy.banner(bubbleOn = false, micOn = true, snoozed = true)
        ).isEqualTo(HomeBannerPolicy.Banner.REPAIR_A11Y)
    }

    @Test
    fun mic_if_a11y_ok() {
        assertThat(
            HomeBannerPolicy.banner(bubbleOn = true, micOn = false, snoozed = false)
        ).isEqualTo(HomeBannerPolicy.Banner.ALLOW_MIC)
    }

    @Test
    fun snooze_if_ready() {
        assertThat(
            HomeBannerPolicy.banner(bubbleOn = true, micOn = true, snoozed = true)
        ).isEqualTo(HomeBannerPolicy.Banner.END_SNOOZE)
    }

    @Test
    fun none_when_clear() {
        assertThat(
            HomeBannerPolicy.banner(bubbleOn = true, micOn = true, snoozed = false)
        ).isEqualTo(HomeBannerPolicy.Banner.NONE)
    }

    @Test
    fun repair_copy_points_to_accessibility() {
        val c = HomeBannerPolicy.copy(HomeBannerPolicy.Banner.REPAIR_A11Y)
        assertThat(c.title).isEqualTo("Turn on Flow Bubble")
        assertThat(c.body).isEqualTo("Open Accessibility, enable Open Flow, then return.")
        assertThat(c.cta).isEqualTo("Open Accessibility")
        assertThat(c.a11yLabel).contains("Flow Bubble")
    }

    @Test
    fun mic_copy_clear() {
        val c = HomeBannerPolicy.copy(HomeBannerPolicy.Banner.ALLOW_MIC)
        assertThat(c.title).isEqualTo("Allow microphone")
        assertThat(c.cta).isEqualTo("Allow microphone")
        assertThat(c.body).contains("bubble")
    }

    @Test
    fun snooze_copy_has_end_cta() {
        val c = HomeBannerPolicy.copy(HomeBannerPolicy.Banner.END_SNOOZE)
        assertThat(c.title).isEqualTo("Bubble snoozed")
        assertThat(c.cta).isEqualTo("End snooze")
        assertThat(c.body).isNull()
    }

    @Test
    fun none_copy_empty() {
        val c = HomeBannerPolicy.copy(HomeBannerPolicy.Banner.NONE)
        assertThat(c.title).isEmpty()
        assertThat(c.cta).isNull()
        assertThat(c.body).isNull()
    }

    @Test
    fun repairA11y_cta_isOpenAccessibility() {
        assertThat(HomeBannerPolicy.copy(HomeBannerPolicy.Banner.REPAIR_A11Y).cta)
            .isEqualTo("Open Accessibility")
    }

    @Test
    fun cta_verbs_never_ok_or_continue_for_a11y_mic_snooze() {
        val forbidden = setOf("OK", "Continue")
        listOf(
            HomeBannerPolicy.Banner.REPAIR_A11Y,
            HomeBannerPolicy.Banner.ALLOW_MIC,
            HomeBannerPolicy.Banner.END_SNOOZE,
        ).forEach { banner ->
            val cta = HomeBannerPolicy.copy(banner).cta
            assertThat(cta).isNotNull()
            assertThat(cta).isNotIn(forbidden)
        }
    }
}
