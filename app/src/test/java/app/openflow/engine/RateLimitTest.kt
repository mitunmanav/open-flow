package app.openflow.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RateLimitTest {

    @Test
    fun default_allows_thirty_per_minute() {
        val now = mutableLong(0L)
        val limit = RateLimit(nowMs = { now.value })
        repeat(RateLimit.DEFAULT_PER_MINUTE) {
            assertThat(limit.tryAcquire("openai")).isEqualTo(RateLimitResult.Allowed)
        }
        val denied = limit.tryAcquire("openai")
        assertThat(denied).isInstanceOf(RateLimitResult.Denied::class.java)
        assertThat((denied as RateLimitResult.Denied).retryAfterMs).isGreaterThan(0L)
    }

    @Test
    fun denied_is_result_not_silent() {
        val now = mutableLong(0L)
        val limit = RateLimit(perMinute = 1, nowMs = { now.value })
        assertThat(limit.tryAcquire("grok")).isEqualTo(RateLimitResult.Allowed)
        val denied = limit.tryAcquire("grok")
        assertThat(denied).isNotEqualTo(RateLimitResult.Allowed)
        assertThat(denied).isInstanceOf(RateLimitResult.Denied::class.java)
    }

    @Test
    fun refill_after_one_minute() {
        val now = mutableLong(0L)
        val limit = RateLimit(perMinute = 2, nowMs = { now.value })
        assertThat(limit.tryAcquire("openai")).isEqualTo(RateLimitResult.Allowed)
        assertThat(limit.tryAcquire("openai")).isEqualTo(RateLimitResult.Allowed)
        assertThat(limit.tryAcquire("openai")).isInstanceOf(RateLimitResult.Denied::class.java)
        now.value = 60_000L
        assertThat(limit.tryAcquire("openai")).isEqualTo(RateLimitResult.Allowed)
    }

    @Test
    fun providers_have_separate_buckets() {
        val now = mutableLong(0L)
        val limit = RateLimit(perMinute = 1, nowMs = { now.value })
        assertThat(limit.tryAcquire("openai")).isEqualTo(RateLimitResult.Allowed)
        assertThat(limit.tryAcquire("grok")).isEqualTo(RateLimitResult.Allowed)
        assertThat(limit.tryAcquire("openai")).isInstanceOf(RateLimitResult.Denied::class.java)
        assertThat(limit.tryAcquire("grok")).isInstanceOf(RateLimitResult.Denied::class.java)
    }

    @Test
    fun default_cap_is_thirty() {
        assertThat(RateLimit.DEFAULT_PER_MINUTE).isEqualTo(30)
    }

    private fun mutableLong(start: Long) = object {
        var value: Long = start
    }
}
