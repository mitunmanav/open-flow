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
 * Structural rules only (time / number / date / weekday / short content chunk):
 *  - "meet at 4:30 actually 5:30" → "meet at 5:30"
 *  - "budget 50k, actually make that 75k" → "budget 75k"
 *  - "let's meet Tuesday wait no Friday" → "let's meet Friday"
 *  - "I want to... scratch that start with the budget" → "start with the budget"
 *  - "Send it to John. No, send it to James." → "send it to James."
 *
 * Bare "no" / "wait" in normal speech stay put ("I have no time").
 * No structured fix → original words stay (never drop the marker).
 */
object CourseCorrector {

    private val ws = Regex("\\s+")

    /**
     * Strong rethink phrases first. Bare no/wait only when comma/sentence punct
     * marks a correction (", no," / ". No,").
     */
    private val markerRegex = Regex(
        """(?i)(?:(?:,\s*|\s+|^)(?:scratch\s+that|on\s+second\s+thought|change\s+(?:that\s+)?to|make\s+that|forget\s+that|forget\s+it|never\s+mind|wait\s+no|no\s+wait|or\s+wait|or\s+rather|i\s+meant|i\s+mean|hang\s+on|hold\s+on|actually|instead|rather|sorry|correction)\s*,?\s+|(?:,\s*|\.\s+|!\s+|\?\s+|^\s*)(?:no|wait)\s*,\s+)"""
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

    private val weekdayPattern = Regex(
        """(?i)\b(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\b"""
    )

    private val makeThat = Regex("""(?i)^make\s+that\s+(.+)$""")
    private val changeTo = Regex("""(?i)^(?:change\s+(?:that\s+)?to\s+)(.+)$""")

    private val functionHead = setOf(
        "about", "of", "to", "for", "that", "this", "the", "a", "an",
        "my", "your", "his", "her", "our", "their", "me", "him", "us", "them",
        "it", "with", "from", "as", "if", "when", "because", "and", "or",
        "so", "but", "on", "in", "at"
    )

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
        t = normalizeSpaces(t)
        t = applyFrameRestate(t)
        return CourseCorrectResult(t, corrections)
    }

    private data class Step(val text: String, val correction: Correction? = null)

    private fun applyOnce(text: String): Step? {
        val m = markerRegex.find(text) ?: return null
        val before = text.substring(0, m.range.first).trim().trimEnd(',', '.', ';', ':')
        var after = text.substring(m.range.last + 1).trim()

        val markerRaw = m.value.trim().trim(',', ' ').lowercase().replace(ws, " ")
        val marker = normalizeMarker(markerRaw)

        if (after.isEmpty()) {
            return if (isRestart(marker) && before.isNotBlank()) {
                Step(
                    before,
                    Correction(originalText = text.trim(), replacementText = before, marker = marker)
                )
            } else {
                null
            }
        }

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

        if (isRestart(marker)) {
            return Step(
                after,
                Correction(
                    originalText = before,
                    replacementText = after,
                    marker = marker
                )
            )
        }

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

        leadingMatch(after, weekdayPattern)?.let { afterDay ->
            val lastDay = weekdayPattern.findAll(before).lastOrNull()
            if (lastDay != null) {
                return Step(
                    spliceEntity(before, lastDay.range, after, afterDay),
                    Correction(
                        originalText = lastDay.value.trim(),
                        replacementText = afterDay.value.trim(),
                        marker = marker
                    )
                )
            }
        }

        val afterWords = after.split(ws).filter { it.isNotBlank() }
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
            if (allowsChunkReplace(marker, afterWords)) {
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
        }

        if (afterWords.size >= 3 ||
            marker.contains("i mean") ||
            marker.contains("i meant") ||
            marker == "instead" ||
            marker.contains("wait no") ||
            marker.contains("no wait") ||
            marker.contains("or wait") ||
            marker == "rather" ||
            marker.contains("hang on") ||
            marker.contains("hold on")
        ) {
            if (afterWords.size >= 3 && before.split(ws).size >= 2) {
                if (afterWords.size <= 3) {
                    if (allowsChunkReplace(marker, afterWords)) {
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
                }
                if (looksLikeFullRestart(before, after) || afterWords.size >= 4) {
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
            }
            if (allowsChunkReplace(marker, afterWords)) {
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
        }

        // No structured fix — keep original (do not drop the marker word).
        return null
    }

    private fun isRestart(marker: String): Boolean =
        marker.contains("scratch") ||
            marker.contains("never mind") ||
            marker.contains("forget") ||
            marker.contains("second thought")

    /**
     * Last-chunk replace is a real swap (name / option / color), not discourse.
     * "actually world" is not a swap. "sorry about that" is not a swap.
     */
    private fun allowsChunkReplace(marker: String, afterWords: List<String>): Boolean {
        if (afterWords.isEmpty()) return false
        val head = afterWords.first().trimEnd('.', ',', '!', '?').lowercase()
        if (head in functionHead) return false
        if (marker == "actually" || marker == "correction") {
            return afterWords.size in 1..2 && looksLikeSlot(afterWords)
        }
        return marker == "instead" ||
            marker.contains("rather") ||
            marker.contains("change") ||
            marker.contains("make that") ||
            marker.contains("i mean") ||
            marker.contains("i meant") ||
            marker == "sorry" ||
            marker.contains("wait no") ||
            marker.contains("no wait") ||
            marker.contains("or wait") ||
            marker.contains("hang on") ||
            marker.contains("hold on") ||
            marker == "no" ||
            marker == "wait"
    }

    private fun looksLikeSlot(words: List<String>): Boolean {
        val joined = words.joinToString(" ")
        if (weekdayPattern.containsMatchIn(joined)) return true
        if (timePattern.containsMatchIn(joined)) return true
        if (moneyOrNumber.containsMatchIn(joined)) return true
        val first = words.first().trimEnd('.', ',', '!', '?')
        if (first.length == 1 && first[0].isLetter()) return true
        return first.firstOrNull()?.isUpperCase() == true
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
        val t = m.trim().lowercase().replace(ws, " ")
        return when {
            t.contains("scratch") -> "scratch that"
            t.contains("second thought") -> "on second thought"
            t.contains("change") -> "change that to"
            t.contains("make that") -> "make that"
            t.contains("forget") -> "forget that"
            t.contains("never mind") -> "never mind"
            t.contains("wait no") -> "wait no"
            t.contains("no wait") -> "no wait"
            t.contains("or wait") -> "or wait"
            t.contains("or rather") -> "or rather"
            t.contains("i meant") -> "i meant"
            t.contains("i mean") -> "i mean"
            t.contains("hang on") -> "hang on"
            t.contains("hold on") -> "hold on"
            t.contains("actually") -> "actually"
            t.contains("instead") -> "instead"
            t.contains("rather") -> "rather"
            t.contains("sorry") -> "sorry"
            t.contains("correction") -> "correction"
            t == "wait" || t.startsWith("wait") -> "wait"
            t == "no" || t.startsWith("no") -> "no"
            else -> t
        }
    }

    /** After restarts same action (shared verb / "send it to X"). */
    private fun looksLikeFullRestart(before: String, after: String): Boolean {
        val bw = before.lowercase().split(ws).filter { it.isNotBlank() }
        val aw = after.lowercase().split(ws).filter {
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
        val tokens = before.trim().split(ws).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return after
        val cleanAfter = after.trim()
        if (cleanAfter.isEmpty()) return before
        val afterWords = cleanAfter.split(ws).filter { it.isNotBlank() }
        val n = afterWords.size.coerceAtLeast(1).coerceAtMost(tokens.size)
        val cut = tokens.size - n
        val head = tokens.subList(0, cut).joinToString(" ")
        return if (head.isBlank()) cleanAfter else "$head $cleanAfter".trim()
    }

    private fun normalizeSpaces(t: String) = t.replace(ws, " ").trim()

    /**
     * Same-frame restatement after ellipsis: "as a gift… as a present" → "as a present".
     * Ambiguous (no shared as-a/the frame) stays put.
     */
    private val frameRestate = Regex(
        """(?i)\b((?:as an?|the)\s+)([A-Za-z]{3,})(\s*(?:\.{2,}|…)\s*)\1([A-Za-z]{3,})\b"""
    )

    internal fun applyFrameRestate(t: String): String {
        val m = frameRestate.find(t) ?: return t
        val old = m.groupValues[2]
        val neu = m.groupValues[4]
        if (old.equals(neu, ignoreCase = true)) return t
        return t.replaceRange(m.range, m.groupValues[1] + neu)
    }
}
