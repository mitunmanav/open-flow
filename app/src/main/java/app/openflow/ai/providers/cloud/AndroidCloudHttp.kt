package app.openflow.ai.providers.cloud

import app.openflow.ai.providers.host.HostUrl
import app.openflow.engine.HttpBackoff
import app.openflow.engine.RateLimit
import app.openflow.engine.RateLimitResult
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Header redact + URL gate. Never log Authorization or keys. */
object CloudHttpSafe {
    const val CONNECT_MS = 15_000
    const val READ_MS = 30_000

    fun allowUrl(url: String): Boolean = HostUrl.allow(url)

    fun hostOf(url: String): String = try {
        URL(url).host.orEmpty()
    } catch (_: Exception) {
        ""
    }

    /** Denied → [IOException] "rate limited". Bucket key is the URL host. */
    fun rateGate(url: String, rateLimit: RateLimit) {
        when (rateLimit.tryAcquire(hostOf(url))) {
            is RateLimitResult.Denied -> throw IOException("rate limited")
            RateLimitResult.Allowed -> Unit
        }
    }

    fun isSecretHeader(name: String): Boolean {
        val n = name.lowercase()
        return n == "authorization" || n == "x-api-key" || n == "api-key" || n.endsWith("api-key")
    }

    fun redactHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (k, v) -> if (isSecretHeader(k)) "REDACTED" else v }
}

/** CloudHttp via HttpURLConnection. HTTPS (LAN HTTP via [HostUrl]). Timeouts. No key logs. */
class AndroidCloudHttp(
    private val rateLimit: RateLimit = RateLimit(),
    private val sleepMs: (Long) -> Unit = { Thread.sleep(it) },
) : CloudHttp {
    override fun post(url: String, headers: Map<String, String>, json: String): String {
        if (!CloudHttpSafe.allowUrl(url)) throw IOException("blocked url")
        CloudHttpSafe.rateGate(url, rateLimit)
        var attempt = 0
        while (true) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                instanceFollowRedirects = false
                connectTimeout = CloudHttpSafe.CONNECT_MS
                readTimeout = CloudHttpSafe.READ_MS
                for ((k, v) in headers) setRequestProperty(k, v)
            }
            val code: Int
            val body: String
            val retryAfter: Long?
            try {
                conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                retryAfter = HttpBackoff.parseRetryAfter(conn.getHeaderField("Retry-After"))
            } finally {
                conn.disconnect()
            }
            if (code in 200..299) return body
            if (!HttpBackoff.shouldRetry(code, attempt)) throw IOException("http $code")
            sleepMs(HttpBackoff.delayMs(attempt, retryAfter))
            attempt++
        }
    }
}
