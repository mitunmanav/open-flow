package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PostStopChipsTest {

    @Test
    fun copy_visible_with_text_before_10s() {
        val s = PostStopChips.state(
            elapsedMs = 0L,
            hasSessionText = true,
            insertOk = true,
            canUndo = false,
        )
        assertThat(s.copy).isTrue()
        assertThat(s.paste).isFalse()
        assertThat(s.undo).isFalse()
        assertThat(s.any).isTrue()
    }

    @Test
    fun hidden_at_10s() {
        val s = PostStopChips.state(
            elapsedMs = 10_000L,
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
    fun still_visible_at_9999() {
        val s = PostStopChips.state(
            elapsedMs = 9_999L,
            hasSessionText = true,
            insertOk = true,
            canUndo = true,
        )
        assertThat(s.copy).isTrue()
        assertThat(s.undo).isTrue()
    }

    @Test
    fun no_text_hides_copy() {
        val s = PostStopChips.state(
            elapsedMs = 100L,
            hasSessionText = false,
            insertOk = false,
            canUndo = false,
        )
        assertThat(s.copy).isFalse()
        assertThat(s.any).isFalse()
    }

    @Test
    fun insert_fail_shows_paste_not_undo() {
        val s = PostStopChips.state(
            elapsedMs = 50L,
            hasSessionText = true,
            insertOk = false,
            canUndo = false,
        )
        assertThat(s.copy).isTrue()
        assertThat(s.paste).isTrue()
        assertThat(s.undo).isFalse()
    }

    @Test
    fun insert_ok_with_snapshot_shows_undo_not_paste() {
        val s = PostStopChips.state(
            elapsedMs = 50L,
            hasSessionText = true,
            insertOk = true,
            canUndo = true,
        )
        assertThat(s.undo).isTrue()
        assertThat(s.paste).isFalse()
        assertThat(s.copy).isTrue()
    }
}
