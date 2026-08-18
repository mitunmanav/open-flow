package app.openflow.prefs

import app.openflow.bubble.BubbleIconPolicy
import app.openflow.bubble.BubbleScaleSteps
import app.openflow.ui.theme.BubbleTint
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
    fun icon_uri_content_or_local_file() {
        assertThat(BubbleIconPolicy.validUri("content://foo")).isTrue()
        assertThat(BubbleIconPolicy.validUri("https://x")).isFalse()
        assertThat(BubbleIconPolicy.validUri("")).isFalse()
        val local = BubbleIconPolicy.localFile(java.io.File("/tmp"))
        assertThat(BubbleIconPolicy.validUri(local.toURI().toString())).isTrue()
        assertThat(BubbleIconPolicy.validUri("file:///tmp/other.png")).isFalse()
    }

    @Test
    fun tint_vector_only_when_no_custom_icon() {
        assertThat(BubbleIconPolicy.useColorFilter("")).isTrue()
        assertThat(BubbleIconPolicy.useColorFilter("content://foo")).isFalse()
    }

    @Test
    fun decode_sample_caps_huge_photos() {
        assertThat(BubbleIconPolicy.decodeSampleSize(4000, 3000, 256)).isAtLeast(8)
        assertThat(BubbleIconPolicy.decodeSampleSize(64, 64, 256)).isEqualTo(1)
    }

    @Test
    fun empty_hex_palette_follows_bubble_tint() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.bubbleTint = BubbleTint.SKY
        assertThat(p.palette().bubbleIdleArgb).isEqualTo(BubbleTint.argb(BubbleTint.SKY))
        assertThat(p.palette().bubbleTextArgb).isEqualTo(BubbleTint.onArgb(BubbleTint.SKY))
    }
}
