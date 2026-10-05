package dev.openflow.dictation.core.refiner

/**
 * Where a run of text came from, and therefore whether app style (stage 8) may write
 * to it.
 *
 * This is a two-value answer to one question. It is deliberately **not** also the
 * answer to "may a later stage match inside this text", because that is a per-stage
 * fact about the pipeline's order — stage 7 rescans stage 6's authored bodies, never
 * its own insertions — and an enum that carried both would be a lie about what it
 * controls. Stage 7's ordering rule is a property of it being one pass over its
 * input, not something a segment records.
 *
 * Both [DICTIONARY][RefinerEntryKind.DICTIONARY] and
 * [SNIPPET][RefinerEntryKind.SNIPPET] bodies are [AUTHORED]. That is the user's own
 * spelling, so `ios → iphone` stays lowercase under Formal: a user who wants style's
 * help has no unprotected path, and that is consistent.
 */
enum class Origin {
    DICTATED,
    AUTHORED,
}

/**
 * A run of text carrying its [origin].
 *
 * Adjacent runs of the same origin are merged, so a segment is a maximal run: it is
 * never half one origin and half the other. A match that spans a boundary between two
 * origins therefore produces one segment of the new origin covering the whole range,
 * not two partial ones — that is a consequence of the merging, not a special case in
 * the matcher.
 */
data class OriginSegment(val text: String, val origin: Origin)

/**
 * A refined transcript as ordered [segments], plus the assembled [text] the next
 * stage reads.
 *
 * **The output is a list, and that is load-bearing rather than stylistic.** Stage 8
 * writes only to dictated segments, and it may not infer that protection by searching
 * for text equal to a stored body: dictated words identical to an authored body must
 * still be styled. A `String` cannot express which run is which, so a single-string
 * return would force stage 8 into exactly that forbidden inference — the only way to
 * recover the distinction from a string is to compare it against stored bodies, which
 * is the defect the type exists to prevent.
 *
 * Authored text is preserved exactly: capitalization, punctuation, spaces, line breaks
 * and blank lines all pass through untouched, including at joins and at the output
 * edges. A terminal [Origin.AUTHORED] segment is not a sentence break.
 */
class RefinedText private constructor(val segments: List<OriginSegment>) {

    /**
     * The segments joined, which is the text the next stage reads for context.
     *
     * Available because the next stage *reads* the text and does not edit it. It is not
     * an alternative to the segments for anything that writes.
     */
    val text: String = segments.joinToString("") { it.text }

    /** Whether there is no text at all, so a later stage has nothing to work on. */
    val isEmpty: Boolean get() = text.isEmpty()

    override fun toString(): String =
        "RefinedText(" + segments.joinToString("") { "${it.origin}«${it.text}»" } + ")"

    companion object {
        /** All-dictated text, which is what stage 5 hands to stage 6. */
        fun dictated(text: String): RefinedText = of(listOf(OriginSegment(text, Origin.DICTATED)))

        /**
         * [segments] with adjacent runs of one origin merged into a single run, and
         * empty runs dropped.
         *
         * The constructor is private so this is the only way in, which is what makes
         * "a segment is a maximal run of one origin" true of every [RefinedText] that
         * exists rather than only of the ones the matcher happens to build. Two
         * adjacent segments of the same origin say the same thing about the text
         * twice, and nothing downstream reads the difference — but a stage 8 that
         * edited each segment separately would edit twice.
         */
        fun of(segments: List<OriginSegment>): RefinedText {
            val merged = mutableListOf<OriginSegment>()
            for (segment in segments) {
                if (segment.text.isEmpty()) continue
                val last = merged.lastOrNull()
                if (last != null && last.origin == segment.origin) {
                    merged[merged.size - 1] = last.copy(text = last.text + segment.text)
                } else {
                    merged += segment
                }
            }
            return RefinedText(merged)
        }
    }
}

/**
 * Accumulates segments, merging adjacent runs of the same origin.
 *
 * Internal rather than public because the only producer is [EntryMatcher]: a builder
 * that callers could also append to would let stage 8 mark its own output, which is
 * the one thing origin exists to prevent.
 */
internal class RefinedTextBuilder {

    private val parts = mutableListOf<OriginSegment>()

    /** Appends [text] as one run of [origin], merging into the previous run if it agrees. */
    fun append(text: String, origin: Origin) {
        if (text.isEmpty()) return
        val last = parts.lastOrNull()
        if (last != null && last.origin == origin) {
            parts[parts.size - 1] = last.copy(text = last.text + text)
        } else {
            parts += OriginSegment(text, origin)
        }
    }

    /** Appends an already-built run, merging it into the previous run if it agrees. */
    fun append(segment: OriginSegment) = append(segment.text, segment.origin)

    /**
     * Appends the characters of [input] in `[from, to)` verbatim, keeping each one's
     * origin. This is the "nothing outside the matched range is touched" rule: a range
     * that starts or ends mid-segment clips the segment, and a range that spans a
     * boundary carries both origins into separate runs.
     */
    fun appendRange(input: RefinedText, from: Int, to: Int) {
        var segmentStart = 0
        for (segment in input.segments) {
            val segmentEnd = segmentStart + segment.text.length
            // Offsets within this segment, so the slice is taken from the segment's own
            // text rather than from the assembled text.
            val localFrom = maxOf(segmentStart, from) - segmentStart
            val localTo = minOf(segmentEnd, to) - segmentStart
            segmentStart = segmentEnd
            if (localTo <= localFrom) continue
            append(segment.text.substring(localFrom, localTo), segment.origin)
        }
    }

    fun build(): RefinedText = RefinedText.of(parts)
}