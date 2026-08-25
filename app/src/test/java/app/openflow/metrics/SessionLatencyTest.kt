package app.openflow.metrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionLatencyTest {

    private var t = 0L
    private val trace = SessionLatency(now = { t })

    @Test
    fun deltas_are_cumulative_between_marks() {
        trace.mark("listen")
        t = 200
        trace.mark("ear_ready")
        t = 500
        trace.mark("first_partial")
        assertThat(trace.elapsedMs("listen", "ear_ready")).isEqualTo(200)
        assertThat(trace.elapsedMs("ear_ready", "first_partial")).isEqualTo(300)
        assertThat(trace.elapsedMs("listen", "first_partial")).isEqualTo(500)
    }

    @Test
    fun first_mark_wins_per_stage() {
        trace.mark("stop")
        t = 100
        trace.mark("stop") // ignored — keeps first timestamp
        assertThat(trace.elapsedMs("stop", "stop")).isEqualTo(0)
        t = 300
        trace.mark("inserted")
        assertThat(trace.elapsedMs("stop", "inserted")).isEqualTo(300)
    }

    @Test
    fun missing_stage_returns_null() {
        trace.mark("listen")
        assertThat(trace.elapsedMs("listen", "inserted")).isNull()
    }

    @Test
    fun clock_regression_clamps_to_zero() {
        trace.mark("listen")
        t = -50
        trace.mark("ear_ready")
        assertThat(trace.elapsedMs("listen", "ear_ready")).isEqualTo(0)
    }

    @Test
    fun summary_lists_stage_gaps() {
        trace.mark("listen")
        t = 120
        trace.mark("ear_ready")
        t = 470
        trace.mark("first_partial")
        assertThat(trace.summary()).isEqualTo("listen=0ms ear_ready=120ms first_partial=350ms")
    }

    @Test
    fun empty_trace_summary_is_blank() {
        assertThat(SessionLatency(now = { 0L }).summary()).isEmpty()
    }
}
