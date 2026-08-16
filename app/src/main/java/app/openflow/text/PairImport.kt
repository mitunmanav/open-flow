package app.openflow.text

/**
 * Parse `from,to` CSV/TSV for dictionary or snippet import.
 * Word cannot be both dict key and snippet trigger.
 */
object PairImport {
    data class Row(val from: String, val to: String)
    data class ParseResult(val rows: List<Row>, val skipped: Int)
    data class Outcome(val added: Int, val skipped: Int, val conflicts: Int)

    enum class Kind { DICT, SNIPPET }
    enum class Decision { ADD, SKIP_DUP, CONFLICT }

    fun parse(text: String): ParseResult {
        val rows = ArrayList<Row>()
        var skipped = 0
        for (rawLine in text.split('\n', '\r')) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                if (line.startsWith("#")) skipped++
                continue
            }
            val cols = splitRow(line)
            if (cols.isEmpty()) {
                skipped++
                continue
            }
            val from = cols[0].trim()
            if (from.isEmpty()) {
                skipped++
                continue
            }
            if (cols.size == 2 && from.equals("from", ignoreCase = true) &&
                cols[1].trim().equals("to", ignoreCase = true)
            ) {
                skipped++
                continue
            }
            val to = if (cols.size >= 2) cols[1].trim().ifBlank { from } else from
            rows += Row(from, to)
        }
        return ParseResult(rows, skipped)
    }

    fun decide(
        from: String,
        existingDict: Set<String>,
        existingSnip: Set<String>,
        kind: Kind,
    ): Decision {
        val key = from.trim()
        if (key.isEmpty()) return Decision.SKIP_DUP
        val dictHit = existingDict.any { it.equals(key, ignoreCase = true) }
        val snipHit = existingSnip.any { it.equals(key, ignoreCase = true) }
        return when (kind) {
            Kind.DICT -> when {
                snipHit -> Decision.CONFLICT
                dictHit -> Decision.SKIP_DUP
                else -> Decision.ADD
            }
            Kind.SNIPPET -> when {
                dictHit -> Decision.CONFLICT
                snipHit -> Decision.SKIP_DUP
                else -> Decision.ADD
            }
        }
    }

    private fun splitRow(line: String): List<String> {
        if (line.contains('\t') && !line.contains(',')) {
            return line.split('\t', limit = 2)
        }
        return splitCsv(line)
    }

    private fun splitCsv(line: String): List<String> {
        val out = ArrayList<String>(2)
        val buf = StringBuilder()
        var i = 0
        var quoted = false
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (quoted && i + 1 < line.length && line[i + 1] == '"') {
                        buf.append('"')
                        i++
                    } else {
                        quoted = !quoted
                    }
                }
                c == ',' && !quoted -> {
                    out += buf.toString()
                    buf.clear()
                }
                else -> buf.append(c)
            }
            i++
        }
        out += buf.toString()
        return out
    }
}
