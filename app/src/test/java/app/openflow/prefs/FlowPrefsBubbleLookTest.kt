package app.openflow.prefs

import app.openflow.bubble.BubbleIconPolicy
import app.openflow.bubble.BubbleScaleSteps
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlowPrefsBubbleLookTest {
    @Test
    fun look_defaults() {
        val p = FlowPrefs(MemoryPrefsStore())
        assertThat(p.bubbleShowCancel).isTrue()
        assertThat(p.bubbleShowDone).isTrue()
        assertThat(p.bubbleIconUri).isEmpty()
        assertThat(p.bubbleRoundPct).isEqualTo(50)
        assertThat(p.bubbleShrinkIdle).isFalse()
        assertThat(p.bubbleShrinkDot).isFalse()
        assertThat(p.bubbleShrinkSearch).isFalse()
    }

    @Test
    fun opacity_clamped() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.bubbleOpacity = 0.05f
        assertThat(p.bubbleOpacity).isEqualTo(0.20f)
        p.bubbleOpacity = 1.5f
        assertThat(p.bubbleOpacity).isEqualTo(1.00f)
    }

    @Test
    fun reset_scale_only() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.bubbleScale = 1.15f
        p.bubbleShrinkIdle = true
        p.resetBubbleScale()
        assertThat(p.bubbleScale).isEqualTo(BubbleScaleSteps.DEFAULT)
        assertThat(p.bubbleShrinkIdle).isTrue()
    }

    @Test
    fun icon_uri_content_only() {
        assertThat(BubbleIconPolicy.validUri("content://foo")).isTrue()
        assertThat(BubbleIconPolicy.validUri("https://x")).isFalse()
        assertThat(BubbleIconPolicy.validUri("")).isFalse()
    }
}
