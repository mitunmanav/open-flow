package app.openflow.ai.providers.cloud

import app.openflow.ai.providers.host.HostUrl
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Header redact + URL gate. Never log Authorization or keys. */
object CloudHttpSafe {
    const val CONNECT_MS = 15_000
    const val READ_MS = 30_000

    fun allowUrl(url: String): Boolean = HostUrl.allow(url)

    fun isSecretHeader(name: String): Boolean {
        val n = name.lowercase()
        return n == "authorization" || n == "x-api-key" || n == "api-key" || n.endsWith("api-key")
    }

    fun redactHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (k, v) -> if (isSecretHeader(k)) "REDACTED" else v }
}

/** CloudHttp via HttpURLConnection. HTTPS (LAN HTTP via [HostUrl]). Timeouts. No key logs. */
class AndroidCloudHttp : CloudHttp {
    override fun post(url: String, headers: Map<String, String>, json: String): String {
        if (!CloudHttpSafe.allowUrl(url)) throw IOException("blocked url")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            instanceFollowRedirects = false
            connectTimeout = CloudHttpSafe.CONNECT_MS
            readTimeout = CloudHttpSafe.READ_MS
            for ((k, v) in headers) setRequestProperty(k, v)
        }
        return try {
            conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("http $code")
            body
        } finally {
            conn.disconnect()
        }
    }
}
