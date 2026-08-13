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
}
