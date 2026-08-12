package app.openflow.text

/**
 * Result of course-correction analysis: cleaned text + each explicit fix.
 */
data class CourseCorrectResult(
    val text: String,
    val corrections: List<Correction> = emptyList()
)

/**
 * Local Wispr-style course correction (no cloud).
 * Turns mid-sentence rethinks into final intent before field insert.
 *
 * Examples:
 *  - "meet at 4:30 actually 5:30" → "meet at 5:30"
 *  - "budget 50k, actually make that 75k" → "budget 75k"
 *  - "let's meet Tuesday wait no Friday" → "let's meet Friday"
 *  - "I want to... scratch that start with the budget" → "start with the budget"
 *  - "The amount is 430, actually 530." → "The amount is 530."
 *  - "Send it to John. No, send it to James." → "send it to James."
 */
object CourseCorrector {

    /**
     * Markers (longer first). Optional comma glue on either side so
     * "430, actually 530" and "12th, no, 15th" match.
     */
    private val markerRegex = Regex(
        """(?i)(?:,\s*|\s+|^)(?:scratch\s+that|change\s+(?:that\s+)?to|make\s+that|wait\s+no|no\s+wait|i\s+mean|actually|instead|rather|sorry|wait|no)\s*,?\s+"""
    )

    private val timePattern = Regex(
        """(?i)\b(?:[01]?\d|2[0-3])(?::[0-5]\d)?\s*(?:am|pm|a\.m\.|p\.m\.)?\b|\b(?:noon|midnight)\b"""
    )

    private val moneyOrNumber = Regex(
        """(?i)\b\d+(?:\.\d+)?\s*[kmb]?\b|\b\d+\s*(?:dollars?|bucks|percent|%)\b"""
    )

    private val datePattern = Regex(
        """(?i)\b(?:(?:january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\.?\s+)?\d{1,2}(?:st|nd|rd|th)?\b"""
    )

    private val makeThat = Regex("""(?i)^make\s+that\s+(.+)$""")
    private val changeTo = Regex("""(?i)^(?:change\s+(?:that\s+)?to\s+)(.+)$""")

    /** Compatibility: cleaned string only. */
    fun apply(raw: String): String = analyze(raw).text

    /**
     * Apply course corrections and record each [Correction].
     */
    fun analyze(raw: String): CourseCorrectResult {
        if (raw.isBlank()) return CourseCorrectResult("")
        var t = raw.trim()
        val corrections = mutableListOf<Correction>()
        var guard = 0
        while (guard++ < 8) {
            val step = applyOnce(t) ?: break
            if (step.text == t) break
            if (step.correction != null) corrections += step.correction
            t = step.text
        }
        return CourseCorrectResult(normalizeSpaces(t), corrections)
    }

    private data class Step(val text: String, val correction: Correction? = null)

    private fun applyOnce(text: String): Step? {
        val m = markerRegex.find(text) ?: return null
        val before = text.substring(0, m.range.first).trim().trimEnd(',', '.', ';', ':')
        var after = text.substring(m.range.last + 1).trim()
        if (after.isEmpty()) {
            return if (before.isBlank()) null else Step(before)
        }

        val markerRaw = m.value.trim().trim(',', ' ').lowercase().replace(Regex("\\s+"), " ")
        val marker = normalizeMarker(markerRaw)

        // Strip nested lead-ins already in after
        makeThat.find(after)?.groupValues?.getOrNull(1)?.let { after = it.trim() }
        changeTo.find(after)?.groupValues?.getOrNull(1)?.let { after = it.trim() }

        if (before.isEmpty()) {
            return Step(
                after,
                Correction(
                    originalText = text.trim(),
                    replacementText = after,
                    marker = marker
                )
            )
        }

        // Hard restart markers → take after only
        if (marker.contains("scratch") || marker == "rather" ||
            marker.contains("wait no") || marker.contains("no wait")
        ) {
            return Step(
                after,
                Correction(
                    originalText = before,
                    replacementText = after,
                    marker = marker
                )
            )
        }

        // Time replace: "… 4:30 … actually 5:30"
        val afterTime = timePattern.find(after)
        if (afterTime != null && after.trim().length <= afterTime.value.length + 6) {
            val lastTime = timePattern.findAll(before).lastOrNull()
            if (lastTime != null) {
                val replaced = before.replaceRange(lastTime.range, afterTime.value.trim()) +
                    after.removeRange(afterTime.range).let { rest ->
                        if (rest.isBlank()) "" else " ${rest.trim()}"
                    }.trimEnd()
                return Step(
                    replaced,
                    Correction(
                        originalText = lastTime.value.trim(),
                        replacementText = afterTime.value.trim(),
                        marker = marker
                    )
                )
            }
        }

        // Number / money replace
        val afterNum = moneyOrNumber.find(after)
        if (afterNum != null && after.trim().split(Regex("\\s+")).size <= 4) {
            val lastNum = moneyOrNumber.findAll(before).lastOrNull()
            if (lastNum != null) {
                // Prefer pure number replace over grabbing year-like noise
                val replaced = before.replaceRange(lastNum.range, afterNum.value.trim()) +
                    trailingAfter(after, afterNum.range)
                return Step(
                    replaced.trimEnd(),
                    Correction(
                        originalText = lastNum.value.trim(),
                        replacementText = afterNum.value.trim(),
                        marker = marker
                    )
                )
            }
        }

        // Date / ordinal: "August 12th, no, August 15th"
        val afterDate = datePattern.find(after)
        if (afterDate != null && after.trim().split(Regex("\\s+")).size <= 4) {
            val lastDate = datePattern.findAll(before).lastOrNull()
            if (lastDate != null) {
                val replaced = before.replaceRange(lastDate.range, afterDate.value.trim()) +
                    trailingAfter(after, afterDate.range)
                return Step(
                    replaced.trimEnd(),
                    Correction(
                        originalText = lastDate.value.trim(),
                        replacementText = afterDate.value.trim(),
                        marker = marker
                    )
                )
            }
        }

        // Short after (≤ 6 words): replace last content word / tail phrase
        val afterWords = after.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (afterWords.size in 1..6) {
            val replaced = replaceLastContentChunk(before, after)
            if (replaced != null) {
                // Full rethink sentence with same verb structure → prefer after when long enough
                // e.g. "Send it to John. No, send it to James."
                if (afterWords.size >= 3 && looksLikeFullRestart(before, after)) {
                    return Step(
                        after,
                        Correction(
                            originalText = before,
                            replacementText = after,
                            marker = marker
                        )
                    )
                }
                return Step(
                    replaced,
                    Correction(
                        originalText = before,
                        replacementText = replaced,
                        marker = marker
                    )
                )
            }
        }

        // Long after / full rethink: prefer final intent (Wispr "start over")
        if (afterWords.size >= 3 ||
            marker.contains("actually") ||
            marker.contains("i mean") ||
            marker == "no" ||
            marker == "instead"
        ) {
            if (afterWords.size >= 3 && before.split(Regex("\\s+")).size >= 2) {
                if (afterWords.size <= 3) {
                    replaceLastContentChunk(before, after)?.let { rep ->
                        return Step(
                            rep,
                            Correction(
                                originalText = before,
                                replacementText = rep,
                                marker = marker
                            )
                        )
                    }
                }
                if (after.firstOrNull()?.isLowerCase() == true &&
                    !after.startsWith("at ", true) &&
                    afterWords.size >= 4
                ) {
                    val cap = after.replaceFirstChar { it.uppercase() }
                    return Step(
                        cap,
                        Correction(
                            originalText = before,
                            replacementText = cap,
                            marker = marker
                        )
                    )
                }
                return Step(
                    after,
                    Correction(
                        originalText = before,
                        replacementText = after,
                        marker = marker
                    )
                )
            }
            // Short entity after "no"/"actually" with no typed match: content chunk
            replaceLastContentChunk(before, after)?.let { rep ->
                return Step(
                    rep,
                    Correction(
                        originalText = before,
                        replacementText = rep,
                        marker = marker
                    )
                )
            }
        }

        val joined = "$before $after".trim()
        return if (joined == text.trim()) null else Step(joined)
    }

    private fun trailingAfter(after: String, matchRange: IntRange): String {
        val rest = after.removeRange(matchRange).trim()
        // Keep terminal punctuation only
        return if (rest.isEmpty()) ""
        else if (rest.all { it in ".,!?;:" }) rest
        else " ${rest.trim()}"
    }

    private fun normalizeMarker(m: String): String {
        val t = m.trim().lowercase().replace(Regex("\\s+"), " ")
        return when {
            t.contains("scratch") -> "scratch that"
            t.contains("change") -> "change that to"
            t.contains("make that") -> "make that"
            t.contains("wait no") -> "wait no"
            t.contains("no wait") -> "no wait"
            t.contains("i mean") -> "i mean"
            t.contains("actually") -> "actually"
            t.contains("instead") -> "instead"
            t.contains("rather") -> "rather"
            t.contains("sorry") -> "sorry"
            t == "wait" || t.startsWith("wait") -> "wait"
            t == "no" || t.startsWith("no") -> "no"
            else -> t
        }
    }

    /**
     * After is a full restart of the same action (verb overlap / "send it to X").
     */
    private fun looksLikeFullRestart(before: String, after: String): Boolean {
        val bw = before.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val aw = after.lowercase().split(Regex("\\s+")).filter {
            it.isNotBlank() && it.trimEnd('.', ',', '!', '?').isNotEmpty()
        }
        if (aw.size < 3) return false
        // Shared content words (e.g. send/to) → full clause restart
        val content = setOf("the", "a", "an", "it", "to", "for", "at", "on", "in", "of", "and", "or")
        val bCore = bw.map { it.trimEnd('.', ',', '!', '?') }.filter { it !in content }.toSet()
        val aCore = aw.map { it.trimEnd('.', ',', '!', '?') }.filter { it !in content }
        // Verb-ish first word of after appears in before
        val first = aCore.firstOrNull() ?: return false
        if (first in bCore || bw.any { it.trimEnd('.', ',', '!', '?') == first }) return true
        // "send it to James" after "Send it to John" — share "send"/"to" pattern length
        return aw.size >= 3 && bw.size >= 3 &&
            aw.take(2).map { it.trimEnd('.', ',') } ==
            bw.take(2).map { it.trimEnd('.', ',') }
    }

    /**
     * Replace the last "content" token(s) in [before] with [after].
     * Skips trailing punctuation-only glue.
     */
    private fun replaceLastContentChunk(before: String, after: String): String? {
        val tokens = before.trim().split(Regex("\\s+")).toMutableList()
        if (tokens.isEmpty()) return after
        val function = setOf(
            "at", "to", "for", "the", "a", "an", "on", "in", "of", "and", "or", "is", "be", "by"
        )
        var end = tokens.lastIndex
        while (end > 0 && tokens[end].lowercase().trim(',', '.', '!', '?') in function) {
            end--
        }
        if (end < 0) return null
        val last = tokens[end].trimEnd(',', '.', '!', '?')
        val span = if (end > 0 && last.all { it.isDigit() || it == ':' } &&
            tokens[end - 1].all { it.isDigit() }
        ) {
            end - 1
        } else {
            end
        }
        val head = tokens.subList(0, span).joinToString(" ")
        val cleanAfter = after.trim()
        return if (head.isBlank()) cleanAfter else "$head $cleanAfter".trim()
    }

    private fun normalizeSpaces(t: String) = t.replace(Regex("\\s+"), " ").trim()
}
