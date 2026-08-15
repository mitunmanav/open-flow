package app.openflow.ai.providers.cloud

internal object ChatJson {
    fun escape(s: String): String = buildString(s.length + 8) {
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                in '\u0000'..'\u001f' -> append("\\u${c.code.toString(16).padStart(4, '0')}")
                else -> append(c)
            }
        }
    }

    fun firstString(json: String, field: String): String? {
        val needle = "\"$field\""
        var i = 0
        while (true) {
            val at = json.indexOf(needle, i)
            if (at < 0) return null
            var j = at + needle.length
            while (j < json.length && json[j].isWhitespace()) j++
            if (j < json.length && json[j] == ':') {
                j++
                while (j < json.length && json[j].isWhitespace()) j++
                if (j < json.length && json[j] == '"') {
                    return unescape(readQuoted(json, j))
                }
            }
            i = at + 1
        }
    }

    fun flag(json: String, field: String): Boolean? {
        val needle = "\"$field\""
        val at = json.indexOf(needle)
        if (at < 0) return null
        var j = at + needle.length
        while (j < json.length && json[j].isWhitespace()) j++
        if (j >= json.length || json[j] != ':') return null
        j++
        while (j < json.length && json[j].isWhitespace()) j++
        return when {
            json.startsWith("true", j) -> true
            json.startsWith("false", j) -> false
            else -> null
        }
    }

    private fun readQuoted(json: String, quoteAt: Int): String {
        val out = StringBuilder()
        var i = quoteAt + 1
        while (i < json.length) {
            val c = json[i]
            if (c == '\\' && i + 1 < json.length) {
                out.append(c).append(json[i + 1])
                i += 2
                continue
            }
            if (c == '"') break
            out.append(c)
            i++
        }
        return out.toString()
    }

    private fun unescape(s: String): String = buildString(s.length) {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    '"' -> append('"')
                    '\\' -> append('\\')
                    else -> append(s[i + 1])
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }
}
