package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PulseScheduleTest {

    @Test
    fun listening_ticks_fast() {
        val d = PulseSchedule.nextDelay(
            listening = true, chipsVisible = true, dragging = true, idleMs = 0L,
        )
        assertThat(d).isEqualTo(PulseSchedule.LISTENING_MS)
    }

    @Test
    fun chips_tick_quarter_second() {
        val d = PulseSchedule.nextDelay(
            listening = false, chipsVisible = true, dragging = false, idleMs = 0L,
        )
        assertThat(d).isEqualTo(PulseSchedule.CHIPS_MS)
    }

    @Test
    fun drag_reapplies_soon() {
        val d = PulseSchedule.nextDelay(
            listening = false, chipsVisible = false, dragging = true, idleMs = 60_000L,
        )
        assertThat(d).isEqualTo(PulseSchedule.DRAG_MS)
    }

    @Test
    fun fresh_idle_waits_one_shot_until_shrink_boundary() {
        val d = PulseSchedule.nextDelay(
            listening = false, chipsVisible = false, dragging = false, idleMs = 2_000L,
        )
        assertThat(d).isEqualTo(3_000L)
    }

    @Test
    fun long_idle_heartbeats_slowly() {
        val d = PulseSchedule.nextDelay(
            listening = false, chipsVisible = false, dragging = false, idleMs = 60_000L,
        )
        assertThat(d).isEqualTo(PulseSchedule.IDLE_HEARTBEAT_MS)
    }
}
