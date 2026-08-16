package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleTapPolicyTest {

    @Test
    fun idle_tap_starts() {
        assertThat(
            BubbleTapPolicy.action(
                listening = false,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = false,
                hitDone = false
            )
        ).isEqualTo(BubbleTapPolicy.Action.START)
    }

    @Test
    fun listen_tap_saves() {
        assertThat(
            BubbleTapPolicy.action(
                listening = true,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = false,
                hitDone = false
            )
        ).isEqualTo(BubbleTapPolicy.Action.STOP_SAVE)
    }

    @Test
    fun listen_cancel_discards() {
        assertThat(
            BubbleTapPolicy.action(
                listening = true,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = true,
                hitDone = false
            )
        ).isEqualTo(BubbleTapPolicy.Action.STOP_DISCARD)
    }

    @Test
    fun listen_done_saves() {
        assertThat(
            BubbleTapPolicy.action(
                listening = true,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = false,
                hitDone = true
            )
        ).isEqualTo(BubbleTapPolicy.Action.STOP_SAVE)
    }

    @Test
    fun drag_is_none() {
        assertThat(
            BubbleTapPolicy.action(
                listening = true,
                stopInProgress = false,
                dragged = true,
                longPressFired = false,
                hitCancel = false,
                hitDone = false
            )
        ).isEqualTo(BubbleTapPolicy.Action.NONE)
    }

    @Test
    fun stop_in_progress_is_none() {
        assertThat(
            BubbleTapPolicy.action(
                listening = false,
                stopInProgress = true,
                dragged = false,
                longPressFired = false,
                hitCancel = false,
                hitDone = false
            )
        ).isEqualTo(BubbleTapPolicy.Action.NONE)
    }

    @Test
    fun cancel_wins_over_done() {
        assertThat(
            BubbleTapPolicy.action(
                listening = true,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = true,
                hitDone = true
            )
        ).isEqualTo(BubbleTapPolicy.Action.STOP_DISCARD)
    }

    @Test
    fun gesture_cancelled_is_none() {
        assertThat(
            BubbleTapPolicy.action(
                listening = false,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = false,
                hitDone = false,
                cancelled = true
            )
        ).isEqualTo(BubbleTapPolicy.Action.NONE)
    }

    @Test
    fun idle_long_press_is_none() {
        assertThat(
            BubbleTapPolicy.action(
                listening = false,
                stopInProgress = false,
                dragged = false,
                longPressFired = true,
                hitCancel = false,
                hitDone = false
            )
        ).isEqualTo(BubbleTapPolicy.Action.NONE)
    }

    @Test
    fun listen_long_press_saves() {
        assertThat(
            BubbleTapPolicy.action(
                listening = true,
                stopInProgress = false,
                dragged = false,
                longPressFired = true,
                hitCancel = false,
                hitDone = false
            )
        ).isEqualTo(BubbleTapPolicy.Action.STOP_SAVE)
    }

    @Test
    fun idle_copy_chip_does_not_start() {
        assertThat(
            BubbleTapPolicy.action(
                listening = false,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = false,
                hitDone = false,
                hitCopy = true,
            )
        ).isEqualTo(BubbleTapPolicy.Action.COPY)
    }

    @Test
    fun idle_undo_chip() {
        assertThat(
            BubbleTapPolicy.action(
                listening = false,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = false,
                hitDone = false,
                hitUndo = true,
            )
        ).isEqualTo(BubbleTapPolicy.Action.UNDO)
    }

    @Test
    fun idle_paste_chip() {
        assertThat(
            BubbleTapPolicy.action(
                listening = false,
                stopInProgress = false,
                dragged = false,
                longPressFired = false,
                hitCancel = false,
                hitDone = false,
                hitPaste = true,
            )
        ).isEqualTo(BubbleTapPolicy.Action.PASTE)
    }
}
