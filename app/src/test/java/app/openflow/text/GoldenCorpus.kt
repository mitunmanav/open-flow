package app.openflow.text

import java.io.File

/**
 * Golden corpus for offline cleanup quality.
 *
 * Fixture format: one case per line, fields joined by " ||| ":
 *   category ||| level ||| raw ||| expected
 *
 * The literal two-char escape "\n" in raw/expected decodes to a newline.
 * Expected output is the *ideal* target; current pipeline performance is
 * pinned separately in baseline.json (see [GoldenCorpusTest]).
 */
object GoldenCorpus {

    data class Case(
        val category: String,
        val level: CleanupLevel,
        val raw: String,
        val expected: String,
    )

    private val SEP = " ||| "
    private val ESCAPED_NL = "\\n"

    fun load(path: String = "src/test/resources/golden/corpus.txt"): List<Case> {
        val lines = File(path).readLines(Charsets.UTF_8)
        return lines.mapIndexed { i, line ->
            val t = line.trim()
            if (t.isEmpty()) return@mapIndexed null
            val parts = t.split(SEP)
            check(parts.size == 4) { "golden line ${i + 1} malformed: $line" }
            Case(
                category = parts[0].trim(),
                level = CleanupLevel.fromPref(parts[1].trim()),
                raw = unescape(parts[2]),
                expected = unescape(parts[3]),
            )
        }.filterNotNull()
    }

    private fun unescape(s: String): String = s.replace(ESCAPED_NL, "\n")
}
