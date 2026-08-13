package app.openflow.ai.providers.host

import java.net.URI

/** https any host. http only RFC1918 / localhost / link-local. No file: or public http. */
object HostUrl {
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
            "https" -> true
            "http" -> isLanHttpHost(host)
            else -> false
        }
    }

    private fun isLanHttpHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        ipv4(host)?.let { return isPrivateV4(it) }
        val v6 = host.lowercase()
        if (v6 == "::1") return true
        return isLinkLocalV6(v6)
    }

    private fun ipv4(host: String): IntArray? {
        val parts = host.split('.')
        if (parts.size != 4) return null
        val nums = IntArray(4)
        for (i in 0..3) {
            val n = parts[i].toIntOrNull() ?: return null
            if (n !in 0..255) return null
            nums[i] = n
        }
        return nums
    }

    private fun isPrivateV4(o: IntArray): Boolean {
        val a = o[0]
        val b = o[1]
        return when {
            a == 10 -> true
            a == 127 -> true
            a == 192 && b == 168 -> true
            a == 169 && b == 254 -> true
            a == 172 && b in 16..31 -> true
            else -> false
        }
    }

    private fun isLinkLocalV6(host: String): Boolean {
        // fe80::/10 → first hextet fe80–febf. Compressed forms still start fe8–feb.
        if (host.length < 4) return false
        if (!host.startsWith("fe")) return false
        val third = host[2]
        val fourth = host[3]
        if (third !in '8'..'9' && third != 'a' && third != 'b') return false
        return fourth.isDigit() || fourth in 'a'..'f' || fourth == ':'
    }
}
