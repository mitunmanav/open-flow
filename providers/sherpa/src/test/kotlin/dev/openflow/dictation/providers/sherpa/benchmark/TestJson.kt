package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * A strict JSON parser, for the tests that parse [RunReport.toJson]'s output.
 *
 * **Why not `org.json`.** It is on the unit-test classpath — `android.jar` ships it — but as the
 * *mockable* stub, whose every method throws `RuntimeException("... not mocked")`. Adding
 * `isReturnDefaultValues = true` does not help: it would make the parser return defaults rather
 * than parse, so `getString` would hand back `null` and the assertions would be meaningless.
 * Adding a real `org.json:json` test dependency to avoid ninety lines of parser is the trade this
 * project has already declined elsewhere: `RunReport` hand-writes its JSON precisely so the
 * shipped dependency graph stays free of a library used to format eleven fields, and the test
 * keeping that property means the parser lives here.
 *
 * It is deliberately **strict**: trailing commas, unquoted keys, single quotes, `NaN`, trailing
 * content after the top-level value and control characters inside strings are all rejected. A
 * permissive parser would pass a malformed report, which is the whole failure this test class
 * exists to catch.
 */
internal object TestJson {

    /** Parses [text], throwing [IllegalArgumentException] naming the offset on anything invalid. */
    fun parse(text: String): Any? {
        val parser = Parser(text)
        val value = parser.value()
        parser.skipWhitespace()
        if (!parser.atEnd()) parser.fail("trailing content after the top-level value")
        return value
    }

    /**
     * Walks a dotted path, where a numeric segment indexes an array.
     *
     * `at("cells", 0, "rtf", "median")` reads like the JSON does, which is the point: a test that
     * navigates by string concatenation is a test that breaks when a key is renamed rather than
     * one that says what it is checking.
     */
    fun at(root: Any?, vararg path: Any): Any? {
        var current = root
        for (segment in path) {
            current = when (segment) {
                is String -> (current as? Map<*, *>)?.get(segment)
                    ?: throw AssertionError("no key '$segment' in ${describe(current)}")
                is Int -> (current as? List<*>)?.getOrNull(segment)
                    ?: throw AssertionError("no index $segment in ${describe(current)}")
                else -> throw AssertionError("a path segment is a String or an Int, was ${segment!!::class}")
            }
        }
        return current
    }

    private fun describe(value: Any?): String = when (value) {
        null -> "null"
        is Map<*, *> -> "an object with keys ${value.keys.joinToString(", ")}"
        is List<*> -> "an array of ${value.size}"
        else -> value.toString()
    }

    private class Parser(private val text: String) {
        private var at = 0

        fun atEnd(): Boolean = at >= text.length

        fun fail(why: String): Nothing =
            throw IllegalArgumentException("JSON at offset $at: $why. Near: '${text.drop(at).take(40)}'")

        fun skipWhitespace() {
            while (at < text.length && text[at].isWhitespace()) at++
        }

        fun value(): Any? {
            skipWhitespace()
            if (atEnd()) fail("expected a value")
            return when (text[at]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> string()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> number()
            }
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!text.startsWith(word, at)) fail("expected $word")
            at += word.length
            return value
        }

        private fun obj(): Map<String, Any?> {
            at++ // '{'
            val out = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                at++
                return out
            }
            while (true) {
                skipWhitespace()
                if (peek() != '"') fail("an object key must be a quoted string")
                val key = string()
                skipWhitespace()
                if (peek() != ':') fail("expected ':' after the key '$key'")
                at++
                out[key] = value()
                skipWhitespace()
                when (peek()) {
                    ',' -> at++
                    '}' -> { at++; return out }
                    else -> fail("expected ',' or '}' in an object")
                }
            }
        }

        private fun arr(): List<Any?> {
            at++ // '['
            val out = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                at++
                return out
            }
            while (true) {
                out += value()
                skipWhitespace()
                when (peek()) {
                    ',' -> at++
                    ']' -> { at++; return out }
                    else -> fail("expected ',' or ']' in an array")
                }
            }
        }

        private fun peek(): Char? = if (atEnd()) null else text[at]

        private fun string(): String {
            at++ // opening quote
            val out = StringBuilder()
            while (true) {
                if (atEnd()) fail("a string is not terminated")
                when (val ch = text[at]) {
                    '"' -> { at++; return out.toString() }
                    '\\' -> {
                        at++
                        if (atEnd()) fail("a string ends with a backslash")
                        when (val escape = text[at]) {
                            '"' -> out.append('"')
                            '\\' -> out.append('\\')
                            '/' -> out.append('/')
                            'b' -> out.append('\b')
                            'f' -> out.append('')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                val hex = text.substring(at + 1, minOf(at + 5, text.length))
                                if (hex.length != 4 || hex.any { it.digitToIntOrNull(16) == null }) {
                                    fail("'\\\\u' must be followed by four hex digits, found '$hex'")
                                }
                                out.append(hex.toInt(16).toChar())
                                at += 4
                            }
                            else -> fail("unknown escape '\\$escape'")
                        }
                        at++
                    }
                    else -> {
                        if (ch < ' ') fail("a raw control character (${ch.code}) inside a string")
                        out.append(ch)
                        at++
                    }
                }
            }
        }

        private fun number(): Double {
            val start = at
            if (peek() == '-') at++
            while (!atEnd() && (text[at].isDigit() || text[at] in ".eE+-")) at++
            val slice = text.substring(start, at)
            val value = slice.toDoubleOrNull()
            if (value == null || slice.isEmpty()) fail("'$slice' is not a number")
            if (value.isNaN() || value.isInfinite()) fail("'$slice' is not a finite number")
            return value
        }
    }
}
