package app.openflow.engine

sealed class RateLimitResult {
    data object Allowed : RateLimitResult()
    data class Denied(val retryAfterMs: Long) : RateLimitResult()
}

/** In-memory token bucket per provider id. Denied is a result, never a silent drop. */
class RateLimit(
    private val perMinute: Int = DEFAULT_PER_MINUTE,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val buckets = HashMap<String, Bucket>()

    fun tryAcquire(providerId: String): RateLimitResult = synchronized(this) {
        val now = nowMs()
        val bucket = buckets.getOrPut(providerId) { Bucket(perMinute.toDouble(), now) }
        bucket.refill(now, perMinute)
        if (bucket.tokens >= 1.0) {
            bucket.tokens -= 1.0
            RateLimitResult.Allowed
        } else {
            val need = 1.0 - bucket.tokens
            val msPerToken = 60_000.0 / perMinute.coerceAtLeast(1)
            RateLimitResult.Denied(
                retryAfterMs = kotlin.math.ceil(need * msPerToken).toLong().coerceAtLeast(1L),
            )
        }
    }

    companion object {
        const val DEFAULT_PER_MINUTE = 30
    }

    private class Bucket(var tokens: Double, var lastMs: Long) {
        fun refill(now: Long, perMinute: Int) {
            if (now <= lastMs || perMinute <= 0) return
            tokens = minOf(
                perMinute.toDouble(),
                tokens + (now - lastMs) * perMinute / 60_000.0,
            )
            lastMs = now
        }
    }
}
