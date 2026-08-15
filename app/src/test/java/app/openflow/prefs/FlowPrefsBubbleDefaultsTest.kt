package app.openflow.prefs

import app.openflow.bubble.BubbleChrome
import app.openflow.ui.theme.BubbleTint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlowPrefsBubbleDefaultsTest {

    @Test
    fun fresh_defaults_are_clean_pill_charcoal() {
        val p = FlowPrefs(MemoryPrefsStore())
        assertThat(p.bubbleShape).isEqualTo("pill")
        assertThat(p.bubbleTint).isEqualTo(BubbleTint.CHARCOAL)
        assertThat(p.bubbleRoundness).isEqualTo(BubbleChrome.ROUND_SOFT)
    }

    @Test
    fun normalize_unknown_shape_falls_to_pill() {
        assertThat(FlowPrefs.normalizeBubbleShape("neon")).isEqualTo("pill")
    }
}
