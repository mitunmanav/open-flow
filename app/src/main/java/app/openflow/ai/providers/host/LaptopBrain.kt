package app.openflow.ai.providers.host

import app.openflow.ai.TextAIProvider

fun interface HostPost {
    fun post(url: String, headers: Map<String, String>, json: String): String
}

/**
 * OpenAI-shape chat against a user laptop / LAN URL.
 * Missing or public-http URL → identity. No crash.
 */
class LaptopBrain(
    private val baseUrl: String?,
    private val model: String = "llama3",
    private val apiKey: String? = null,
    private val post: HostPost = HostPost { _, _, _ -> error("no transport") },
) : TextAIProvider {
    override val name: String = "laptop"

    val rewrite: Boolean = true
    val commandMode: Boolean = true
    val needsNet: Boolean = true
    val audioLeavesDevice: Boolean = true

    override suspend fun enhance(text: String, mode: String): String {
        if (!HostUrl.allow(baseUrl)) return text
        val url = chatUrl(baseUrl!!.trim())
        val headers = linkedMapOf("Content-Type" to "application/json")
        val key = apiKey?.trim().orEmpty()
        if (key.isNotEmpty()) headers["Authorization"] = "Bearer $key"
        val body = chatBody(text, mode)
        return try {
            parseContent(post.post(url, headers, body)) ?: text
        } catch (e: Exception) {
            android.util.Log.w("LaptopBrain", "enhance request failed, returning raw text", e)
            text
        }
    }

    private fun chatUrl(base: String): String {
        val trimmed = base.trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) trimmed
        else "$trimmed/chat/completions"
    }

    private fun chatBody(text: String, mode: String): String {
        val system = if (mode == "command") {
            "Follow the user instruction. Do not invent facts."
        } else {
            "Clean this dictation. Do not invent facts."
        }
        return """{"model":${jsonStr(model)},"messages":[{"role":"system","content":${jsonStr(system)}},{"role":"user","content":${jsonStr(text)}}]}"""
    }

    private fun parseContent(json: String): String? {
        val msg = json.indexOf("\"message\"")
        if (msg < 0) return null
        val key = json.indexOf("\"content\"", msg)
        if (key < 0) return null
        val colon = json.indexOf(':', key + 9)
        if (colon < 0) return null
        val start = json.indexOf('"', colon + 1)
        if (start < 0) return null
        return unescapeJson(json, start + 1)
    }

    private fun jsonStr(s: String): String {
        val out = StringBuilder(s.length + 2)
        out.append('"')
        for (c in s) {
            when (c) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (c < ' ') out.append("\\u").append("%04x".format(c.code)) else out.append(c)
            }
        }
        out.append('"')
        return out.toString()
    }

    private fun unescapeJson(src: String, from: Int): String? {
        val out = StringBuilder()
        var i = from
        while (i < src.length) {
            when (val c = src[i]) {
                '"' -> return out.toString()
                '\\' -> {
                    if (i + 1 >= src.length) return null
                    when (val n = src[i + 1]) {
                        '"', '\\', '/' -> {
                            out.append(n)
                            i += 2
                        }
                        'n' -> {
                            out.append('\n')
                            i += 2
                        }
                        'r' -> {
                            out.append('\r')
                            i += 2
                        }
                        't' -> {
                            out.append('\t')
                            i += 2
                        }
                        'u' -> {
                            if (i + 5 >= src.length) return null
                            val cp = src.substring(i + 2, i + 6).toIntOrNull(16) ?: return null
                            out.append(cp.toChar())
                            i += 6
                        }
                        else -> return null
                    }
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return null
    }
}
