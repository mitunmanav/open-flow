package app.openflow.engine

/** 429 / 5xx retry. Honor Retry-After. Never tight-loop. */
object HttpBackoff {
    const val MAX_RETRIES = 3
    const val MAX_DELAY_MS = 30_000L

    private val RETRY_CODES = setOf(429, 500, 502, 503, 504)

    fun shouldRetry(httpCode: Int, attempt: Int): Boolean =
        attempt < MAX_RETRIES && httpCode in RETRY_CODES

    fun delayMs(attempt: Int, retryAfterSec: Long?): Long {
        if (retryAfterSec != null && retryAfterSec > 0) {
            return minOf(retryAfterSec * 1000L, MAX_DELAY_MS)
        }
        val exp = 1_000L shl attempt.coerceIn(0, 10)
        return minOf(exp, MAX_DELAY_MS)
    }

    fun parseRetryAfter(header: String?): Long? {
        val t = header?.trim().orEmpty()
        if (t.isEmpty()) return null
        return t.toLongOrNull()?.takeIf { it > 0 }
    }
}
