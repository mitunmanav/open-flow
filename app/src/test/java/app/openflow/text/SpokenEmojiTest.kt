package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpokenEmojiTest {
    @Test
    fun off_is_identity() {
        assertThat(SpokenEmoji.apply("say smile emoji please", enabled = false))
            .isEqualTo("say smile emoji please")
    }

    @Test
    fun smile_maps() {
        assertThat(SpokenEmoji.apply("say smile emoji please", enabled = true))
            .isEqualTo("say 😄 please")
    }

    @Test
    fun longest_phrase_wins() {
        assertThat(SpokenEmoji.apply("heart eyes emoji", enabled = true))
            .isEqualTo("😍")
    }

    @Test
    fun case_insensitive() {
        assertThat(SpokenEmoji.apply("FIRE EMOJI", enabled = true)).isEqualTo("🔥")
    }
}
