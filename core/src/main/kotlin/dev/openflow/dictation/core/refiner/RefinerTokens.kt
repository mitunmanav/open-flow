package dev.openflow.dictation.core.refiner

import java.util.Locale

/**
 * One token: a maximal run of Unicode letters and digits, in which an apostrophe is
 * internal when it sits between two letters.
 *
 * [start] and [end] are offsets into the string that was tokenized, so a match can
 * replace an exact range of it and nothing outside it. [folded] is [text] case-folded
 * and is the only form the matcher ever compares, which is what makes case and
 * punctuation in a stored trigger non-load-bearing.
 */
data class RefinerToken(
    val text: String,
    val start: Int,
    val end: Int,
    val folded: String,
)

/**
 * The tokenizer behind stages 6 and 7, and the trigger key derived from it.
 *
 * The contract is [Entry matching][entry-matching] in `docs/architecture/refiner.md`,
 * settled by ticket 65. Three of its properties are decisions rather than mechanics,
 * and each one has a test that can fail:
 *
 * - **No accent folding.** [fold] lowercases and stops. `Café` therefore matches
 *   `café`, and `cafe` does not, because a folding comparison would also make
 *   `resume` match `résumé` — a user's `resume` is not their `résumé`.
 * - **No branch on the trigger's shape.** A trigger is a token sequence whether or
 *   not it contains a space, so there is no spelling that is safe and another that
 *   is not. The prototype's `patternFor` branched on exactly that, and it made
 *   `sign off` fire inside "design off" while `sig` was safe inside "signal".
 * - **Tokens carry offsets.** One match replaces from the first matched token's start
 *   to the last token's end, so spacing outside a match survives verbatim.
 *
 * The token positions are UTF-16 offsets, the same indexing `substring` uses, so
 * [RefinerToken.start] and [RefinerToken.end] may be applied to the original string
 * without any further translation.
 *
 * **A known limit, stated rather than hidden.** [fold] is `lowercase(Locale.ROOT)`,
 * not `Normalizer`'s full case folding, and the two differ: Greek final sigma (`ς`)
 * does not fold onto `σ`, and compatibility forms such as `ﬁ` do not decompose to
 * `fi`. Folding them together would mean normalizing, and
 * normalizing would also decompose accented letters — which is the folding this
 * matcher refuses to do. English, V1's default, is unaffected. A script without
 * word spacing has the larger limit: a CJK run is a single token, so a trigger in
 * such a script could only match an entire run.
 *
 * [entry-matching]: ../../../../../../docs/architecture/refiner.md#entry-matching
 */
object RefinerTokens {

    /**
     * Splits [text] into tokens. Every character that is neither a letter, a digit,
     * nor an internal apostrophe separates tokens and is dropped, so tokens are
     * compared by their text rather than by their neighbours.
     */
    fun tokenize(text: String): List<RefinerToken> {
        if (text.isEmpty()) return emptyList()
        val tokens = mutableListOf<RefinerToken>()
        var i = 0
        while (i < text.length) {
            if (!isWordChar(text[i])) {
                i++
                continue
            }
            val start = i
            // Whether the character just consumed was a letter, which is what makes a
            // following apostrophe internal rather than a separator.
            var lastWasLetter = isLetter(text[start])
            var end = start + 1
            while (end < text.length) {
                val c = text[end]
                if (isWordChar(c)) {
                    lastWasLetter = isLetter(c)
                    end++
                    continue
                }
                val continues = isApostrophe(c) && lastWasLetter &&
                    end + 1 < text.length && isLetter(text[end + 1])
                if (!continues) break
                // The apostrophe joins the run; `lastWasLetter` still describes the
                // letter before it, and the loop consumes the letter that follows.
                end++
            }
            val body = text.substring(start, end)
            tokens += RefinerToken(body, start, end, fold(body))
            i = end
        }
        return tokens
    }

    /**
     * Case-folds one token or trigger fragment. Locale-independent, and deliberately
     * no accent folding — see this object's documentation for why, and for the one
     * gap `lowercase` leaves against full Unicode case folding.
     */
    fun fold(text: String): String = text.lowercase(Locale.ROOT)

    /**
     * The token sequence a trigger is compared as. Empty when the trigger holds no
     * word, which is a trigger the settings surface refuses rather than one the
     * matcher tries to honour — see `checkNewEntry`.
     *
     * This is the same derivation the matcher uses, deliberately: a duplicate check
     * that folded or split differently would disagree with the matcher it is
     * protecting.
     */
    fun triggerKey(trigger: String): List<String> = tokenize(trigger).map { it.folded }

    /**
     * `\p{L}` — any Unicode letter. Java's own `Character.isLetter`, which covers
     * Lu, Ll, Lt, Lm and Lo, so an accented or non-Latin letter is a word character
     * like any other and is not special-cased anywhere above.
     */
    private fun isLetter(c: Char): Boolean = Character.isLetter(c)

    /**
     * `\p{N}` — any Unicode number, not only an ASCII digit. `Character.isDigit` would
     * be narrower (it reports only Nd, the decimal digits), and the contract says
     * "letters and digits" of the Unicode kind: a superscript or a Roman-numeral
     * digit is a digit to the user saying it.
     */
    private fun isNumber(c: Char): Boolean = when (Character.getType(c)) {
        Character.DECIMAL_DIGIT_NUMBER.toInt(),
        Character.LETTER_NUMBER.toInt(),
        Character.OTHER_NUMBER.toInt(),
        -> true

        else -> false
    }

    private fun isWordChar(c: Char): Boolean = isLetter(c) || isNumber(c)

    /** Both apostrophes a keyboard or a dictation engine can produce. */
    private fun isApostrophe(c: Char): Boolean = c == '\'' || c == '\u2019'
}