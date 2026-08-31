package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleLabelVisibilityTest {
    @Test
    fun idle_follows_show_text_pref() {
        assertThat(BubbleLabelVisibility.idle(showText = true)).isTrue()
        assertThat(BubbleLabelVisibility.idle(showText = false)).isFalse()
    }

    @Test
    fun listening_follows_show_text_pref() {
        assertThat(BubbleLabelVisibility.listening(showText = true)).isTrue()
        assertThat(BubbleLabelVisibility.listening(showText = false)).isFalse()
    }

    @Test
    fun post_stop_always_hides_label() {
        assertThat(BubbleLabelVisibility.postStop()).isFalse()
    }
}
