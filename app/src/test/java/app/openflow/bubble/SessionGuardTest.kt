package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionGuardTest {

    @Test
    fun none_before_warn() {
        assertThat(SessionGuard.phase(0L)).isEqualTo(SessionPhase.NONE)
        assertThat(SessionGuard.phase(269_999L)).isEqualTo(SessionPhase.NONE)
    }

    @Test
    fun warn_at_four_thirty() {
        assertThat(SessionGuard.phase(270_000L)).isEqualTo(SessionPhase.WARN)
        assertThat(SessionGuard.phase(299_999L)).isEqualTo(SessionPhase.WARN)
    }

    @Test
    fun stop_at_five_minutes() {
        assertThat(SessionGuard.phase(300_000L)).isEqualTo(SessionPhase.STOP)
    }

    @Test
    fun negative_elapsed_is_none() {
        assertThat(SessionGuard.phase(-1L)).isEqualTo(SessionPhase.NONE)
    }

    @Test
    fun remaining_ms_clamps() {
        assertThat(SessionGuard.remainingMs(0L)).isEqualTo(SessionGuard.STOP_MS)
        assertThat(SessionGuard.remainingMs(300_000L)).isEqualTo(0L)
        assertThat(SessionGuard.remainingMs(400_000L)).isEqualTo(0L)
        assertThat(SessionGuard.remainingMs(-5L)).isEqualTo(SessionGuard.STOP_MS)
    }
}
