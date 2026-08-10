package app.openflow.text

/**
 * Local Wispr-style course correction (no cloud).
 * Turns mid-sentence rethinks into final intent before field insert.
 *
 * Examples:
 *  - "meet at 4:30 actually 5:30" → "meet at 5:30"
 *  - "budget 50k, actually make that 75k" → "budget 75k"
 *  - "let's meet Tuesday wait no Friday" → "let's meet Friday"
 *  - "I want to... scratch that start with the budget" → "start with the budget"
 */
object CourseCorrector {

    private val markerRegex = Regex(
        """(?i)(?:\s+|^)(?:actually|wait\s+no|no\s+wait|wait|i\s+mean|scratch\s+that|rather|make\s+that|change\s+(?:that\s+)?to|sorry)\s+"""
    )

    private val timePattern = Regex(
        """(?i)\b(?:[01]?\d|2[0-3])(?::[0-5]\d)?\s*(?:am|pm|a\.m\.|p\.m\.)?\b|\b(?:noon|midnight)\b"""
    )

    private val moneyOrNumber = Regex(
        """(?i)\b\d+(?:\.\d+)?\s*[kmb]?\b|\b\d+\s*(?:dollars?|bucks|percent|%)\b"""
    )

    private val makeThat = Regex("""(?i)^make\s+that\s+(.+)$""")
    private val changeTo = Regex("""(?i)^(?:change\s+(?:that\s+)?to\s+)(.+)$""")

    fun apply(raw: String): String {
        if (raw.isBlank()) return ""
        var t = raw.trim()
        // Keep applying while markers remain (multi-correct in one utterance)
        var guard = 0
        while (guard++ < 8) {
            val next = applyOnce(t)
            if (next == t) break
            t = next
        }
        return normalizeSpaces(t)
    }

    private fun applyOnce(text: String): String {
        val m = markerRegex.find(text) ?: return text
        val before = text.substring(0, m.range.first).trim()
        var after = text.substring(m.range.last + 1).trim()
        if (after.isEmpty()) return before.ifBlank { text }

        // Strip nested lead-ins already in after
        makeThat.find(after)?.groupValues?.getOrNull(1)?.let { after = it.trim() }
        changeTo.find(after)?.groupValues?.getOrNull(1)?.let { after = it.trim() }

        if (before.isEmpty()) return after

        val marker = m.value.trim().lowercase()
        // Hard restart markers → take after only
        if (marker.contains("scratch") || marker == "rather" ||
            marker.contains("wait no") || marker.contains("no wait")
        ) {
            return after
        }

        // Time replace: "… 4:30 … actually 5:30"
        val afterTime = timePattern.find(after)
        if (afterTime != null && after.trim().length <= afterTime.value.length + 6) {
            val lastTime = timePattern.findAll(before).lastOrNull()
            if (lastTime != null) {
                return before.replaceRange(lastTime.range, afterTime.value.trim()) +
                    after.removeRange(afterTime.range).let { rest ->
                        if (rest.isBlank()) "" else " ${rest.trim()}"
                    }.trimEnd()
            }
        }

        // Number / money replace
        val afterNum = moneyOrNumber.find(after)
        if (afterNum != null && after.trim().split(Regex("\\s+")).size <= 4) {
            val lastNum = moneyOrNumber.findAll(before).lastOrNull()
            if (lastNum != null) {
                return before.replaceRange(lastNum.range, afterNum.value.trim())
            }
        }

        // Short after (≤ 6 words): replace last content word / tail phrase
        val afterWords = after.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (afterWords.size in 1..6) {
            // "meet Tuesday wait Friday" style — replace last non-function word cluster
            val replaced = replaceLastContentChunk(before, after)
            if (replaced != null) return replaced
        }

        // Long after / full rethink: prefer final intent (Wispr "start over")
        if (afterWords.size >= 4 || marker.contains("actually") || marker.contains("i mean")) {
            // If after is a full sentence-like restart, use it
            if (afterWords.size >= 3 && before.split(Regex("\\s+")).size >= 2) {
                // Prefer entity replace when after is just a correction fragment
                if (afterWords.size <= 3) {
                    replaceLastContentChunk(before, after)?.let { return it }
                }
                // "actually let's start with budget" → after
                if (after.firstOrNull()?.isLowerCase() == true &&
                    !after.startsWith("at ", true) &&
                    afterWords.size >= 4
                ) {
                    return after.replaceFirstChar { it.uppercase() }
                }
                return after
            }
        }

        return "$before $after".trim()
    }

    /**
     * Replace the last "content" token(s) in [before] with [after].
     * Skips trailing punctuation-only glue.
     */
    private fun replaceLastContentChunk(before: String, after: String): String? {
        val tokens = before.trim().split(Regex("\\s+")).toMutableList()
        if (tokens.isEmpty()) return after
        // Drop trailing function words to find content to replace
        val function = setOf(
            "at", "to", "for", "the", "a", "an", "on", "in", "of", "and", "or", "is", "be", "by"
        )
        var end = tokens.lastIndex
        while (end > 0 && tokens[end].lowercase().trim(',', '.', '!') in function) {
            end--
        }
        if (end < 0) return null
        // Replace last content token (or last two if time-like "4 30")
        val last = tokens[end]
        val span = if (end > 0 && last.all { it.isDigit() || it == ':' } &&
            tokens[end - 1].all { it.isDigit() }
        ) {
            end - 1
        } else {
            end
        }
        val head = tokens.subList(0, span).joinToString(" ")
        return if (head.isBlank()) after else "$head $after".trim()
    }

    private fun normalizeSpaces(t: String) = t.replace(Regex("\\s+"), " ").trim()
}
