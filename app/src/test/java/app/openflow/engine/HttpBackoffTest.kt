package app.openflow.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HttpBackoffTest {

    @Test
    fun retries_429_and_5xx_until_max() {
        assertThat(HttpBackoff.shouldRetry(429, attempt = 0)).isTrue()
        assertThat(HttpBackoff.shouldRetry(502, attempt = 1)).isTrue()
        assertThat(HttpBackoff.shouldRetry(503, attempt = 2)).isTrue()
        assertThat(HttpBackoff.shouldRetry(504, attempt = 0)).isTrue()
        assertThat(HttpBackoff.shouldRetry(429, attempt = HttpBackoff.MAX_RETRIES)).isFalse()
        assertThat(HttpBackoff.shouldRetry(400, attempt = 0)).isFalse()
        assertThat(HttpBackoff.shouldRetry(401, attempt = 0)).isFalse()
        assertThat(HttpBackoff.shouldRetry(200, attempt = 0)).isFalse()
    }

    @Test
    fun retry_after_header_wins_capped() {
        assertThat(HttpBackoff.delayMs(0, retryAfterSec = 5)).isEqualTo(5_000L)
        assertThat(HttpBackoff.delayMs(0, retryAfterSec = 120))
            .isEqualTo(HttpBackoff.MAX_DELAY_MS)
    }

    @Test
    fun exponential_when_no_header() {
        assertThat(HttpBackoff.delayMs(0, retryAfterSec = null)).isEqualTo(1_000L)
        assertThat(HttpBackoff.delayMs(1, retryAfterSec = null)).isEqualTo(2_000L)
        assertThat(HttpBackoff.delayMs(2, retryAfterSec = null)).isEqualTo(4_000L)
    }

    @Test
    fun parse_retry_after_seconds() {
        assertThat(HttpBackoff.parseRetryAfter("7")).isEqualTo(7L)
        assertThat(HttpBackoff.parseRetryAfter(" 12 ")).isEqualTo(12L)
        assertThat(HttpBackoff.parseRetryAfter(null)).isNull()
        assertThat(HttpBackoff.parseRetryAfter("nope")).isNull()
        assertThat(HttpBackoff.parseRetryAfter("")).isNull()
    }
}
