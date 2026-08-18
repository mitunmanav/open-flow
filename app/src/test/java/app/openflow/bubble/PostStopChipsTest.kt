package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PostStopChipsTest {

    @Test
    fun insert_ok_hides_all_chips() {
        val s = PostStopChips.state(
            elapsedMs = 50L,
            hasSessionText = true,
            insertOk = true,
            canUndo = true,
        )
        assertThat(s.copy).isFalse()
        assertThat(s.undo).isFalse()
        assertThat(s.paste).isFalse()
        assertThat(s.any).isFalse()
    }

    @Test
    fun hidden_at_10s() {
        val s = PostStopChips.state(
            elapsedMs = 10_000L,
            hasSessionText = true,
            insertOk = false,
            canUndo = true,
        )
        assertThat(s.copy).isFalse()
        assertThat(s.undo).isFalse()
        assertThat(s.paste).isFalse()
        assertThat(s.any).isFalse()
    }

    @Test
    fun insert_fail_shows_paste_only() {
        val s = PostStopChips.state(
            elapsedMs = 50L,
            hasSessionText = true,
            insertOk = false,
            canUndo = true,
        )
        assertThat(s.copy).isFalse()
        assertThat(s.undo).isFalse()
        assertThat(s.paste).isTrue()
        assertThat(s.any).isTrue()
    }

    @Test
    fun no_text_hides_paste() {
        val s = PostStopChips.state(
            elapsedMs = 100L,
            hasSessionText = false,
            insertOk = false,
            canUndo = false,
        )
        assertThat(s.paste).isFalse()
        assertThat(s.any).isFalse()
    }
}
