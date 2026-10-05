package dev.openflow.dictation.core.refiner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The segmented output's own properties: adjacent runs of one origin merge, the
 * assembled text is the plain concatenation, and a join supplies nothing.
 *
 * These look trivial, which is why they are tested anyway. A `String` return would
 * pass every one of them and still be unable to tell stage 8 what it may not write
 * to, which is the defect the type exists to prevent — so the tests that matter for
 * origin live in `EntryMatcherTest`, which asserts on segments.
 */
class RefinedTextTest {

    @Test
    fun textIsTheSegmentsConcatenated() {
        val refined = RefinedText.of(
            listOf(
                OriginSegment("please review this. ", Origin.DICTATED),
                OriginSegment("Best regards,\n\nMitun", Origin.AUTHORED),
            ),
        )

        assertEquals("please review this. Best regards,\n\nMitun", refined.text)
    }

    @Test
    fun aSegmentBoundaryContributesNothingOfItsOwn() {
        // No separator, no space and no sentence break is supplied at a join: the
        // spacing around a body is dictated text's, and authored whitespace is
        // preserved exactly.
        val refined = RefinedText.of(
            listOf(
                OriginSegment("before", Origin.DICTATED),
                OriginSegment("BODY", Origin.AUTHORED),
                OriginSegment("after", Origin.DICTATED),
            ),
        )

        assertEquals("beforeBODYafter", refined.text)
    }

    @Test
    fun adjacentRunsOfOneOriginBecomeOneSegment() {
        // Two dictated segments are the same statement about the text written twice. A
        // stage 8 that edited each segment separately would then edit twice.
        val refined = RefinedText.of(
            listOf(
                OriginSegment("I", Origin.AUTHORED),
                OriginSegment(" will go", Origin.DICTATED),
                OriginSegment(" now", Origin.DICTATED),
                OriginSegment("!", Origin.AUTHORED),
            ),
        )

        assertEquals(
            listOf(
                OriginSegment("I", Origin.AUTHORED),
                OriginSegment(" will go now", Origin.DICTATED),
                OriginSegment("!", Origin.AUTHORED),
            ),
            refined.segments,
        )
    }

    @Test
    fun anEmptySegmentIsDropped() {
        // A stage that replaced a range with an empty body leaves no empty run behind,
        // so stage 8 is never handed a segment it cannot edit or preserve.
        val refined = RefinedText.of(
            listOf(
                OriginSegment("a", Origin.DICTATED),
                OriginSegment("", Origin.AUTHORED),
                OriginSegment("b", Origin.DICTATED),
            ),
        )

        assertEquals(listOf(OriginSegment("ab", Origin.DICTATED)), refined.segments)
    }

    @Test
    fun aTerminalAuthoredSegmentIsNotASentenceBreak() {
        // Stage 8 must read this and append no full stop: the ending is the user's.
        val refined = RefinedText.of(
            listOf(OriginSegment("Best regards,\n\nMitun\nOpenFlow,", Origin.AUTHORED)),
        )

        assertEquals("Best regards,\n\nMitun\nOpenFlow,", refined.text)
        assertEquals(Origin.AUTHORED, refined.segments.single().origin)
    }

    @Test
    fun trailingWhitespaceDoesNotMakeAnAuthoredEndingEligibleForPunctuation() {
        // The trailing space is authored and is preserved, and it is still authored: a
        // stage 8 that punctuated because the text did not end in a full stop would be
        // reading the wrong thing.
        val refined = RefinedText.of(
            listOf(
                OriginSegment("Mitun", Origin.AUTHORED),
                OriginSegment("  ", Origin.AUTHORED),
            ),
        )

        assertEquals(listOf(OriginSegment("Mitun  ", Origin.AUTHORED)), refined.segments)
        assertEquals("Mitun  ", refined.text)
    }

    @Test
    fun emptyTextIsEmpty() {
        assertTrue(RefinedText.of(emptyList()).isEmpty)
        assertEquals("", RefinedText.dictated("").text)
        assertEquals(emptyList<OriginSegment>(), RefinedText.dictated("").segments)
    }
}