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
 * Mid-sentence rethinks → final intent before field insert.
 *
 * Structural rules only (time / number / date / short content chunk):
 *  - "meet at 4:30 actually 5:30" → "meet at 5:30"
 *  - "budget 50k, actually make that 75k" → "budget 75k"
 *  - "let's meet Tuesday wait no Friday" → "let's meet Friday"
 *  - "I want to... scratch that start with the budget" → "start with the budget"
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

    /** Apply course corrections and record each [Correction]. */
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

        // Explicit restart only — not every wait/no/rather
        if (marker.contains("scratch")) {
            return Step(
                after,
                Correction(
                    originalText = before,
                    replacementText = after,
                    marker = marker
                )
            )
        }

        // Entity replace: if after leads with time/number/date, swap last same type in before
        leadingMatch(after, timePattern)?.let { afterTime ->
            val lastTime = timePattern.findAll(before).lastOrNull()
            if (lastTime != null) {
                return Step(
                    spliceEntity(before, lastTime.range, after, afterTime),
                    Correction(
                        originalText = lastTime.value.trim(),
                        replacementText = afterTime.value.trim(),
                        marker = marker
                    )
                )
            }
        }

        leadingMatch(after, moneyOrNumber)?.let { afterNum ->
            val lastNum = moneyOrNumber.findAll(before).lastOrNull()
            if (lastNum != null) {
                return Step(
                    spliceEntity(before, lastNum.range, after, afterNum),
                    Correction(
                        originalText = lastNum.value.trim(),
                        replacementText = afterNum.value.trim(),
                        marker = marker
                    )
                )
            }
        }

        leadingMatch(after, datePattern)?.let { afterDate ->
            val lastDate = datePattern.findAll(before).lastOrNull()
            if (lastDate != null) {
                return Step(
                    spliceEntity(before, lastDate.range, after, afterDate),
                    Correction(
                        originalText = lastDate.value.trim(),
                        replacementText = afterDate.value.trim(),
                        marker = marker
                    )
                )
            }
        }

        // Short after (≤ 6 words): replace last content word / full clause restart
        val afterWords = after.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (afterWords.size in 1..6) {
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
            replaceLastContentChunk(before, after)?.let { replaced ->
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

        // Longer after / full rethink: prefer final intent
        if (afterWords.size >= 3 ||
            marker.contains("actually") ||
            marker.contains("i mean") ||
            marker == "no" ||
            marker == "instead" ||
            marker.contains("wait no") ||
            marker.contains("no wait") ||
            marker == "rather" ||
            marker == "wait"
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

    /** Entity must lead the correction payload (optional leading punct only). */
    private fun leadingMatch(after: String, pattern: Regex): MatchResult? {
        val m = pattern.find(after) ?: return null
        val head = after.substring(0, m.range.first)
        if (head.isNotEmpty() && !head.all { it.isWhitespace() || it in ",.;:—" }) {
            return null
        }
        return m
    }

    /** Replace [oldRange] in [before] with entity from [afterMatch]; append rest of after. */
    private fun spliceEntity(
        before: String,
        oldRange: IntRange,
        after: String,
        afterMatch: MatchResult
    ): String {
        val entity = afterMatch.value.trim()
        val rest = after.substring(afterMatch.range.last + 1).trim()
        val head = before.replaceRange(oldRange, entity).trimEnd()
        return when {
            rest.isEmpty() -> head
            rest.all { it in ".,!?;:" } -> head + rest
            else -> normalizeSpaces("$head $rest")
        }
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

    /** After restarts same action (shared verb / "send it to X"). */
    private fun looksLikeFullRestart(before: String, after: String): Boolean {
        val bw = before.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val aw = after.lowercase().split(Regex("\\s+")).filter {
            it.isNotBlank() && it.trimEnd('.', ',', '!', '?').isNotEmpty()
        }
        if (aw.size < 3) return false
        val content = setOf("the", "a", "an", "it", "to", "for", "at", "on", "in", "of", "and", "or")
        val bCore = bw.map { it.trimEnd('.', ',', '!', '?') }.filter { it !in content }.toSet()
        val aCore = aw.map { it.trimEnd('.', ',', '!', '?') }.filter { it !in content }
        val first = aCore.firstOrNull() ?: return false
        if (first in bCore || bw.any { it.trimEnd('.', ',', '!', '?') == first }) return true
        return aw.size >= 3 && bw.size >= 3 &&
            aw.take(2).map { it.trimEnd('.', ',') } ==
            bw.take(2).map { it.trimEnd('.', ',') }
    }

    /**
     * Replace trailing content in [before] with [after].
     * Drops last N tokens where N = word count of after (min 1), so
     * "pick option A" + "option B" → "pick option B" (not "pick option option B").
     */
    private fun replaceLastContentChunk(before: String, after: String): String? {
        val tokens = before.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return after
        val cleanAfter = after.trim()
        if (cleanAfter.isEmpty()) return before
        val afterWords = cleanAfter.split(Regex("\\s+")).filter { it.isNotBlank() }
        val n = afterWords.size.coerceAtLeast(1).coerceAtMost(tokens.size)
        val cut = tokens.size - n
        val head = tokens.subList(0, cut).joinToString(" ")
        return if (head.isBlank()) cleanAfter else "$head $cleanAfter".trim()
    }

    private fun normalizeSpaces(t: String) = t.replace(Regex("\\s+"), " ").trim()
}
