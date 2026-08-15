package app.openflow.ai.providers.host

import java.net.URI

/**
 * https any host. http only NSC cleartext literals (no CIDR — Android domain tags
 * cannot express RFC1918 ranges). Keep in sync with network_security_config.xml.
 */
object HostUrl {
    val NSC_CLEARTEXT_HOSTS: Set<String> = setOf(
        "localhost",
        "ip6-localhost",
        "127.0.0.1",
        "::1",
        "10.0.2.2",
        "10.0.0.1",
        "10.0.0.2",
        "172.16.0.1",
        "172.17.0.1",
        "192.168.0.1",
        "192.168.1.1",
        "192.168.1.10",
        "192.168.1.100",
    )

    fun allow(raw: String?): Boolean {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return false
        val uri = try {
            URI(text)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.trim().orEmpty().removePrefix("[").removeSuffix("]")
        if (host.isEmpty()) return false
        return when (scheme) {
            "https", "wss" -> true
            "http", "ws" -> isNscCleartextHost(host)
            else -> false
        }
    }

    private fun isNscCleartextHost(host: String): Boolean {
        val h = host.lowercase()
        if (h in NSC_CLEARTEXT_HOSTS) return true
        return h.endsWith(".localhost") || h.endsWith(".ip6-localhost")
    }
}
