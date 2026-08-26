package app.openflow.text

/**
 * Disfluency repetition handling (replaces naive adjacent-dedupe):
 * - function-word doubles collapse ("the the", "i i")
 * - 2x content words are KEPT (emphasis: "very very good")
 * - 3x+ collapses to one except emphatic words ("no no no" kept) and digit
 *   words (phone numbers reach ITN intact)
 * - comma-separated repeats stay (separate segments: "No, no, I insist")
 * - single-letter stutters collapse ("w w why" → "why" handled upstream by
 *   keeping one letter, dropped by filler pass if noise)
 */
object StutterCollapse {

    private val functionDoubles = setOf(
        "the", "a", "an", "and", "or", "but", "if", "of", "to", "in", "on",
        "it", "its", "is", "was", "were", "be", "been", "am", "are",
        "i", "you", "he", "she", "we", "they", "me", "him", "her", "us", "them",
        "my", "your", "our", "their", "his", "this", "that", "these", "those",
        "can", "could", "should", "would", "will", "shall", "may", "might",
        "do", "does", "did", "have", "has", "had", "let's", "lets", "not", "so",
    )

    /** Keep-when-emphatic: 3x "no" is insistence, not a stutter. */
    private val emphaticKeep = setOf(
        "no", "yes", "yeah", "yep", "nope", "please", "really", "wow", "oh",
    )

    private fun isDigitWord(w: String): Boolean =
        ItnNumber.units[w.lowercase()]?.let { it in 0..9 } == true

    /** Line-local so inserted newlines (voice commands) survive re-passes. */
    fun apply(t: String): String {
        if (!t.contains('\n')) return collapseSegments(t)
        return t.split("\n").joinToString("\n") { line -> collapseSegments(line) }
    }

    private fun collapseSegments(t: String): String {
        // Comma-separated repeats are intentional; process segments apart.
        // Edge commas survive even when a side cleans to empty (idempotence:
        // ", hi" must not become "Hi" — re-runs would then recase the word).
        // Digit groups ("1,200") keep their tight comma: only add the space
        // a spoken separator had.
        val parts = t.split(",")
        val sb = StringBuilder()
        var pendingSpace = false
        parts.forEachIndexed { idx, seg ->
            val cleaned = collapseRun(seg.trim())
            if (idx > 0 && pendingSpace && cleaned.isNotEmpty()) sb.append(' ')
            sb.append(cleaned)
            pendingSpace = if (idx < parts.size - 1) {
                sb.append(',')
                seg.endsWith(" ") || seg.endsWith("\t") ||
                    parts[idx + 1].startsWith(" ") || parts[idx + 1].startsWith("\t")
            } else {
                false
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun collapseRun(seg: String): String {
        if (seg.isBlank()) return seg
        val words = seg.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return seg
        val out = ArrayList<String>(words.size)
        var i = 0
        while (i < words.size) {
            var runEnd = i
            while (runEnd + 1 < words.size &&
                words[runEnd + 1].equals(words[i], ignoreCase = true)
            ) {
                runEnd++
            }
            val runLen = runEnd - i + 1
            val bare = words[i].trim(',', '.', '!', '?', ';', ':').lowercase()
            val singleLetter = bare.length == 1 && bare[0].isLetter()
            val followerDiffers = runEnd + 1 < words.size &&
                !words[runEnd + 1].equals(words[i], ignoreCase = true)

            when {
                // Letter stutter ("w w why"): drop the whole letter run.
                // Articles/pronouns ("a", "i") are real words — never dropped.
                singleLetter && runLen >= 2 && bare !in setOf("a", "i") &&
                    followerDiffers -> {
                    i = runEnd + 1
                }
                isDigitWord(bare) -> {
                    repeat(runLen) { out.add(words[it + i]) }
                    i = runEnd + 1
                }
                // Doubles: function words collapse, content words stay (emphasis).
                runLen == 2 -> {
                    out.add(words[i])
                    if (bare in functionDoubles) {
                        i = runEnd + 1              // "the the" → "the"
                    } else {
                        out.add(words[i + 1])       // emphasis kept
                        i = runEnd + 1
                    }
                }
                runLen >= 3 && bare in emphaticKeep -> {
                    val kept = (0 until runLen).map { words[i + it] }
                    if (runLen == 3) {
                        val joined = kept.joinToString(", ")
                        out.add(if (followerDiffers) "$joined," else joined)
                    } else {
                        kept.forEach { out.add(it) }
                        if (followerDiffers) {
                            out[out.lastIndex] = out.last() + ","
                        }
                    }
                    i = runEnd + 1
                }
                runLen >= 3 -> {                     // triple+ → one
                    out.add(words[i])
                    i = runEnd + 1
                }
                else -> {
                    out.add(words[i])
                    i++
                }
            }
        }
        return out.joinToString(" ")
    }
}
