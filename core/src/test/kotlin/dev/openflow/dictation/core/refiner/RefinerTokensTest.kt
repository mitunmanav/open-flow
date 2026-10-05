package dev.openflow.dictation.core.refiner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tokenizer's rules, one test per rule that could be got wrong.
 *
 * Each of these names a case where a plausible implementation would answer
 * differently, because a tokenizer test that only checks the happy path has not shown
 * it can fail.
 */
class RefinerTokensTest {

    private fun words(text: String) = RefinerTokens.tokenize(text).map { it.text }

    private fun folded(text: String) = RefinerTokens.triggerKey(text)

    @Test
    fun anApostropheBetweenLettersIsInternalSoDontIsOneToken() {
        assertEquals(listOf("don't"), words("don't"))
        assertEquals(listOf("it's"), words("it's"))
        // The typographic apostrophe is the same character to a speaker.
        assertEquals(listOf("don’t"), words("don’t"))
    }

    @Test
    fun anApostropheThatIsNotBetweenLettersSeparates() {
        // Leading and trailing apostrophes are quoting or emphasis, not one word.
        assertEquals(listOf("quoted"), words("'quoted'"))
        assertEquals(listOf("quoted"), words("“quoted”"))
        // A doubled apostrophe is two separators, not one internal one.
        assertEquals(listOf("don", "t"), words("don''t"))
        // Between a letter and a digit the apostrophe is not between *letters*.
        assertEquals(listOf("ab", "12"), words("ab'12"))
        // And after a digit it is not between letters either: `5'11` is a dimension,
        // two tokens, and not one word with an apostrophe in the middle of it.
        assertEquals(listOf("5", "ft"), words("5'ft"))
        assertEquals(listOf("2", "3", "4"), words("2'3'4"))
    }

    @Test
    fun anApostropheBetweenTwoLettersIsInternalHoweverManySurroundIt() {
        // The positive case the digit cases above are measured against, including the
        // typographic apostrophe.
        assertEquals(listOf("rock'n'roll"), words("rock'n'roll"))
        assertEquals(listOf("rock’n’roll"), words("rock’n’roll"))
        assertEquals(listOf("it's"), words("it's"))
    }

    @Test
    fun aHyphenSeparatesTokensSoSignOffIsTwo() {
        assertEquals(listOf("sign", "off"), words("sign-off"))
        // The prototype stand-in's defect, stated as a tokenizer fact: `design` is one
        // token, so `sign off` has nothing to match there.
        assertEquals(listOf("design", "off"), words("design off"))
        assertEquals(listOf("e", "mail"), words("e-mail"))
    }

    @Test
    fun cafeAuLaitSplitsOnItsHyphensAndKeepsItsAccent() {
        assertEquals(listOf("café", "au", "lait"), words("café-au-lait"))
        // Same words without the accent: three tokens that are three *different* keys.
        assertEquals(listOf("cafe", "au", "lait"), words("cafe-au-lait"))
    }

    @Test
    fun lettersAndDigitsFormOneTokenButSeparatorsSplitThem() {
        assertEquals(listOf("iphone15"), words("iphone15"))
        assertEquals(listOf("iphone", "15"), words("iphone 15"))
        assertEquals(listOf("24", "7"), words("24/7"))
    }

    @Test
    fun tokenOffsetsAddressTheOriginalText() {
        val text = "  please sign off  now"
        val tokens = RefinerTokens.tokenize(text)
        assertEquals(listOf("please", "sign", "off", "now"), tokens.map { it.text })
        assertEquals("please", text.substring(tokens[0].start, tokens[0].end))
        assertEquals("off", text.substring(tokens[2].start, tokens[2].end))
        // Offsets are what make a replaced range exact, so the leading two spaces and
        // the double space after the match are addressable and therefore untouched.
        assertEquals(2, tokens[0].start)
        assertEquals(17, tokens[2].end)
    }

    @Test
    fun foldingIsCaseInsensitiveAndAccentSensitive() {
        assertEquals(listOf("sign", "off"), folded("SIGN OFF"))
        assertEquals(folded("café"), folded("Café"))
        // The load-bearing one. Folding accents would make `resume` match `résumé`.
        assertFalse(folded("cafe") == folded("café"))
        assertFalse(folded("resume") == folded("résumé"))
    }

    @Test
    fun aTriggerKeyIgnoresPunctuationAndSpacingButNotAccents() {
        assertEquals(folded("sign off"), folded("Sign-Off!"))
        assertEquals(folded("sign off"), folded("  sign   off  "))
        assertEquals(folded("sign off"), folded("’sign‘\noff"))
    }

    @Test
    fun aTriggerWithNoWordHasAnEmptyKey() {
        assertTrue(folded("").isEmpty())
        assertTrue(folded("   ").isEmpty())
        assertTrue(folded("!?!-").isEmpty())
        // A digit is a word: a trigger may be a number the user says out loud.
        assertEquals(listOf("24"), folded("24"))
    }

    @Test
    fun tokensAreMaximalRuns() {
        // Separators are dropped, not emitted: no whitespace, punctuation or quote
        // tokens, because the matcher only ever compares runs of letters and digits.
        assertEquals(listOf("a", "b", "c"), words("a, b; c"))
        assertTrue(RefinerTokens.tokenize("").isEmpty())
        assertTrue(RefinerTokens.tokenize("...!?").isEmpty())
    }
}