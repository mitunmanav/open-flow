package app.openflow.text

/**
 * Spoken-command phrase data loaded from voice_commands.json (classpath).
 * Single source of truth for punctuation, layout, and edit phrases.
 */
class PhraseMap(
    val insert: Map<String, String>,
    val edit: Map<String, EditOp>,
) {
    /** Longest phrase word-count (for look-ahead). */
    val maxPhraseWords: Int =
        (insert.keys + edit.keys).maxOfOrNull { it.split(' ').size } ?: 1

    /**
     * Longest phrase match at [index] in [tokens] (lowercase compare).
     * Prefers longer phrases; insert and edit share the same walk.
     */
    fun matchAt(tokens: List<String>, index: Int): Match? {
        if (index >= tokens.size) return null
        val maxLen = minOf(maxPhraseWords, tokens.size - index)
        for (len in maxLen downTo 1) {
            val phrase = tokens.subList(index, index + len).joinToString(" ").lowercase()
            insert[phrase]?.let { return Match.Insert(len, it) }
            edit[phrase]?.let { return Match.Edit(len, it) }
        }
        return null
    }

    sealed class Match {
        abstract val wordCount: Int

        data class Insert(override val wordCount: Int, val symbol: String) : Match()
        data class Edit(override val wordCount: Int, val op: EditOp) : Match()
    }

    enum class EditOp { WORD, CHAR, SENTENCE, CLEAR }

    companion object {
        private const val RESOURCE = "voice_commands.json"

        val default: PhraseMap by lazy { loadDefault() }

        fun loadDefault(): PhraseMap {
            val cl = PhraseMap::class.java.classLoader
            val stream = cl?.getResourceAsStream(RESOURCE)
                ?: error("Missing classpath resource $RESOURCE (ship assets + test/resources)")
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parse(text)
        }

        /** Parse voice_commands.json subset (objects + strings only). */
        fun parse(json: String): PhraseMap {
            val root = JsonMini.parseObject(json.trim())
            val punct = stringMap(root, "punctuation")
            val layout = stringMap(root, "layout")
            val editRaw = stringMap(root, "edit")
            val insert = LinkedHashMap<String, String>(punct.size + layout.size)
            for ((k, v) in punct) insert[k.lowercase()] = v
            for ((k, v) in layout) insert[k.lowercase()] = v
            val edit = LinkedHashMap<String, EditOp>(editRaw.size)
            for ((k, v) in editRaw) {
                edit[k.lowercase()] = when (v.lowercase()) {
                    "word" -> EditOp.WORD
                    "char" -> EditOp.CHAR
                    "sentence" -> EditOp.SENTENCE
                    "clear" -> EditOp.CLEAR
                    else -> error("Unknown edit op '$v' for phrase '$k'")
                }
            }
            return PhraseMap(insert, edit)
        }

        @Suppress("UNCHECKED_CAST")
        private fun stringMap(root: Map<String, Any?>, key: String): Map<String, String> {
            val raw = root[key] as? Map<*, *>
                ?: error("voice_commands.json missing object '$key'")
            val out = LinkedHashMap<String, String>(raw.size)
            for ((k, v) in raw) {
                require(k is String && v is String) { "Expected string entries in '$key'" }
                out[k] = v
            }
            return out
        }
    }
}

/**
 * Minimal JSON object/string parser for phrase maps (no full JSON lib on JVM tests).
 */
internal object JsonMini {
    fun parseObject(input: String): Map<String, Any?> {
        val p = Parser(input)
        p.skipWs()
        val obj = p.parseValue()
        p.skipWs()
        require(p.done) { "Trailing junk in JSON at ${p.i}" }
        @Suppress("UNCHECKED_CAST")
        return obj as? Map<String, Any?> ?: error("Root must be object")
    }

    private class Parser(private val s: String) {
        var i = 0
        val done: Boolean get() = i >= s.length

        fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseValue(): Any? {
            skipWs()
            require(i < s.length) { "Unexpected end of JSON" }
            return when (s[i]) {
                '{' -> parseObject()
                '"' -> parseString()
                else -> error("Unexpected '${s[i]}' at $i")
            }
        }

        fun parseObject(): Map<String, Any?> {
            require(s[i] == '{') { "Expected '{' at $i" }
            i++
            val out = LinkedHashMap<String, Any?>()
            skipWs()
            if (i < s.length && s[i] == '}') {
                i++
                return out
            }
            while (true) {
                skipWs()
                require(i < s.length && s[i] == '"') { "Expected key string at $i" }
                val key = parseString()
                skipWs()
                require(i < s.length && s[i] == ':') { "Expected ':' after key at $i" }
                i++
                val value = parseValue()
                out[key] = value
                skipWs()
                require(i < s.length) { "Unclosed object" }
                when (s[i]) {
                    ',' -> {
                        i++
                        continue
                    }
                    '}' -> {
                        i++
                        return out
                    }
                    else -> error("Expected ',' or '}' at $i")
                }
            }
        }

        fun parseString(): String {
            require(s[i] == '"') { "Expected '\"' at $i" }
            i++
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        require(i < s.length) { "Bad escape at end" }
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                require(i + 4 <= s.length) { "Bad unicode escape" }
                                val hex = s.substring(i, i + 4)
                                sb.append(hex.toInt(16).toChar())
                                i += 4
                            }
                            else -> error("Bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            error("Unclosed string")
        }
    }
}
