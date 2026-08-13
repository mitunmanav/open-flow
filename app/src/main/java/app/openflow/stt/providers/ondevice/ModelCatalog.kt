package app.openflow.stt.providers.ondevice

/** In-APK catalog parser. Upstream URLs only — we do not host model files. */
object ModelCatalog {

    fun parse(json: String): List<CatalogModel> {
        val p = Scan(json.trim())
        p.skipWs()
        val root = p.parseValue()
        p.skipWs()
        require(p.done) { "trailing junk in catalog JSON" }
        val rows = root as? List<*> ?: error("catalog root must be an array")
        return rows.map { row ->
            val obj = row as? Map<*, *> ?: error("catalog entry must be an object")
            obj.toModel()
        }
    }

    private fun Map<*, *>.toModel(): CatalogModel {
        val id = string("id")
        val url = string("url")
        require(id.isNotBlank()) { "catalog id is blank" }
        require(url.startsWith("https://")) { "catalog url must be https" }
        return CatalogModel(
            id = id,
            minRamMb = int("minRamMb"),
            minFreeMb = int("minFreeMb"),
            quality = int("quality"),
            url = url,
        )
    }

    private fun Map<*, *>.string(key: String): String =
        this[key] as? String ?: error("catalog entry missing string '$key'")

    private fun Map<*, *>.int(key: String): Int {
        val v = this[key] ?: error("catalog entry missing '$key'")
        return when (v) {
            is Int -> v
            is Long -> v.toInt()
            else -> error("catalog '$key' must be an int")
        }
    }

    /** Tiny scanner: array / object / string / int / bool / null. JVM tests have no org.json. */
    private class Scan(private val s: String) {
        var i = 0
        val done: Boolean get() = i >= s.length

        fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseValue(): Any? {
            skipWs()
            require(i < s.length) { "unexpected end of catalog JSON" }
            return when (val c = s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                '-', in '0'..'9' -> parseNumber()
                't', 'f' -> parseBool()
                'n' -> parseNull()
                else -> error("unexpected '$c' at $i")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            require(s[i] == '{')
            i++
            val out = LinkedHashMap<String, Any?>()
            skipWs()
            if (i < s.length && s[i] == '}') {
                i++
                return out
            }
            while (true) {
                skipWs()
                require(i < s.length && s[i] == '"') { "expected key at $i" }
                val key = parseString()
                skipWs()
                require(i < s.length && s[i] == ':') { "expected ':' at $i" }
                i++
                out[key] = parseValue()
                skipWs()
                require(i < s.length) { "unclosed object" }
                when (s[i]) {
                    ',' -> i++
                    '}' -> {
                        i++
                        return out
                    }
                    else -> error("expected ',' or '}' at $i")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            require(s[i] == '[')
            i++
            val out = ArrayList<Any?>()
            skipWs()
            if (i < s.length && s[i] == ']') {
                i++
                return out
            }
            while (true) {
                out += parseValue()
                skipWs()
                require(i < s.length) { "unclosed array" }
                when (s[i]) {
                    ',' -> i++
                    ']' -> {
                        i++
                        return out
                    }
                    else -> error("expected ',' or ']' at $i")
                }
            }
        }

        private fun parseString(): String {
            require(s[i] == '"')
            i++
            val sb = StringBuilder()
            while (i < s.length) {
                when (val c = s[i++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        require(i < s.length) { "bad escape" }
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                require(i + 4 <= s.length) { "bad unicode escape" }
                                sb.append(s.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> error("bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            error("unclosed string")
        }

        private fun parseNumber(): Int {
            val start = i
            if (s[i] == '-') i++
            require(i < s.length && s[i].isDigit()) { "bad number at $start" }
            while (i < s.length && s[i].isDigit()) i++
            return s.substring(start, i).toInt()
        }

        private fun parseBool(): Boolean {
            return when {
                s.startsWith("true", i) -> {
                    i += 4
                    true
                }
                s.startsWith("false", i) -> {
                    i += 5
                    false
                }
                else -> error("bad bool at $i")
            }
        }

        private fun parseNull(): Any? {
            require(s.startsWith("null", i)) { "bad null at $i" }
            i += 4
            return null
        }
    }
}
