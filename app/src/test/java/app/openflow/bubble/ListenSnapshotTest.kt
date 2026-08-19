package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ListenSnapshotTest {

    private fun snap(
        finals: String = "hello",
        partial: String = "world",
        retry: String? = null,
        start: Long = 1000L,
    ) = ListenSnapshot(
        generation = 3,
        finals = finals,
        lastPartial = partial,
        prefix = "Dear ",
        earId = "system",
        sessionId = "sid",
        startedAtElapsed = start,
        startedWallMs = 50L,
        retrySessionId = retry,
    )

    @Test
    fun raw_uses_session_text() {
        assertThat(snap().raw).isEqualTo(SessionText.commitRaw("hello", "world"))
        assertThat(snap(finals = "", partial = "").raw).isEmpty()
    }

    @Test
    fun duration_and_retry() {
        assertThat(snap(start = 1000L).durationMs(1400L)).isEqualTo(400L)
        assertThat(snap(retry = null).isRetry).isFalse()
        assertThat(snap(retry = "old").isRetry).isTrue()
    }
}
