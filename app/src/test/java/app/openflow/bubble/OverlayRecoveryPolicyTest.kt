package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OverlayRecoveryPolicyTest {
    @Test
    fun retries_three_times_with_backoff() {
        assertThat(OverlayRecoveryPolicy.DELAYS_MS)
            .containsExactly(1_000L, 5_000L, 15_000L).inOrder()
    }

    @Test
    fun shouldRetry_within_bounds() {
        assertThat(OverlayRecoveryPolicy.shouldRetry(attempt = 0)).isTrue()
        assertThat(OverlayRecoveryPolicy.shouldRetry(attempt = 2)).isTrue()
        assertThat(OverlayRecoveryPolicy.shouldRetry(attempt = 3)).isFalse()
    }

    @Test
    fun exhausted_after_all_delays() {
        assertThat(OverlayRecoveryPolicy.exhausted(attempt = 3)).isTrue()
        assertThat(OverlayRecoveryPolicy.exhausted(attempt = 2)).isFalse()
    }

    @Test
    fun delay_for_attempt_is_sequential() {
        assertThat(OverlayRecoveryPolicy.delayFor(0)).isEqualTo(1_000L)
        assertThat(OverlayRecoveryPolicy.delayFor(1)).isEqualTo(5_000L)
        assertThat(OverlayRecoveryPolicy.delayFor(2)).isEqualTo(15_000L)
        assertThat(OverlayRecoveryPolicy.delayFor(9)).isEqualTo(15_000L)
    }
}
