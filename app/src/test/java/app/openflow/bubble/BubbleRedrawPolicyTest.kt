package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleRedrawPolicyTest {
    @Test
    fun visibility_throttles_within_interval() {
        assertThat(BubbleRedrawPolicy.shouldRefreshVisibility(1000L, 1020L)).isFalse()
        assertThat(BubbleRedrawPolicy.shouldRefreshVisibility(1000L, 1060L)).isTrue()
        assertThat(BubbleRedrawPolicy.shouldRefreshVisibility(1000L, 1010L, force = true)).isTrue()
    }

    @Test
    fun rms_label_skips_same_bars_inside_interval() {
        assertThat(
            BubbleRedrawPolicy.shouldUpdateRmsLabel("▮▮▯▯", "▮▮▯▯", 1000L, 1010L)
        ).isFalse()
        assertThat(
            BubbleRedrawPolicy.shouldUpdateRmsLabel("▮▮▯▯", "▮▮▮▯", 1000L, 1010L)
        ).isTrue()
    }
}
