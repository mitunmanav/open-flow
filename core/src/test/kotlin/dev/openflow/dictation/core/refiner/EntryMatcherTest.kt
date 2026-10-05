package dev.openflow.dictation.core.refiner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The matcher rules of [Entry matching][entry-matching], one test per rule, each with
 * the case that would fail if the rule were implemented differently.
 *
 * Tests assert on the *segments*, not just the joined text, wherever the rule is about
 * origin — a `String` assertion cannot tell a dictated run from an authored one, which
 * is the whole reason the return type is a list.
 *
 * [entry-matching]: ../../../../../../../docs/architecture/refiner.md#entry-matching
 */
class EntryMatcherTest {

    private val whatsapp = "com.whatsapp"
    private val gmail = "com.google.android.gm"

    private fun dict(trigger: String, body: String, scope: EntryScope = EntryScope.EveryApp) =
        RefinerEntry(RefinerEntryKind.DICTIONARY, trigger, body, scope)

    private fun snippet(trigger: String, body: String, scope: EntryScope = EntryScope.EveryApp) =
        RefinerEntry(RefinerEntryKind.SNIPPET, trigger, body, scope)

    private fun match(vararg entries: RefinerEntry) = EntryMatcher(entries.toList())

    private fun dictionary(text: String, vararg entries: RefinerEntry, target: String = whatsapp) =
        match(*entries).applyDictionary(text, target)

    private fun snippets(input: RefinedText, vararg entries: RefinerEntry, target: String = whatsapp) =
        match(*entries).applySnippets(input, target)

    private fun dictated(text: String) = OriginSegment(text, Origin.DICTATED)

    private fun authored(text: String) = OriginSegment(text, Origin.AUTHORED)

    // ── specificity: the rule added mid-grill, and the one that needed adding ──────

    /**
     * The contradicting case. Longest-match-wins alone gives `sign off`; nearest-scope
     * alone gives `sign`. The app's own entry must win even though the global one is
     * longer, which is only true if specificity filters *before* the scan.
     */
    @Test
    fun anAppsOwnTriggerWinsOverALongerGlobalOne() {
        val result = dictionary(
            "i will sign off",
            dict("sign", "bye", EntryScope.App(whatsapp)),
            dict("sign off", "Goodbye."),
        )

        assertEquals(listOf(dictated("i will "), authored("bye"), dictated(" off")), result.segments)
    }

    @Test
    fun specificityWinsEvenAgainstAGlobalTriggerThatStartsEarlier() {
        // The global entry begins first, so a scan that takes the first hit at the
        // earliest position would choose it.
        val result = dictionary(
            "sign off now",
            dict("sign off", "Goodbye."),
            dict("sign", "bye", EntryScope.App(whatsapp)),
        )

        assertEquals("bye off now", result.text)
    }

    @Test
    fun anEntryForAnotherAppIsNeverApplicable() {
        // WhatsApp's `sig` must not reach a Gmail dictation, where a global `sig` then
        // wins by length rather than by scope.
        val result = dictionary(
            "i will sign off",
            dict("sign", "bye", EntryScope.App(whatsapp)),
            dict("sig", "ok"),
            target = gmail,
        )

        // No app-scoped candidate at this position, so the global `sig` is considered —
        // and finds nothing. `sign` and `sign off` are both app-scoped, so neither is.
        assertEquals(listOf(dictated("i will sign off")), result.segments)
    }

    @Test
    fun theLongestMatchAtAPositionWinsWhenNoAppEntryApplies() {
        val result = dictionary(
            "i will sign off now",
            dict("sign", "bye"),
            dict("sign off", "Goodbye."),
        )

        assertEquals("i will Goodbye. now", result.text)
    }

    // ── whole-token matching: the defect the prototype stand-in had ────────────────

    @Test
    fun aTriggerDoesNotFireInsideALongerToken() {
        // `sig` is safe inside `signal` because `signal` is one token.
        val result = dictionary("the signal is weak", dict("sig", "Kubernetis"))

        assertEquals(listOf(dictated("the signal is weak")), result.segments)
    }

    @Test
    fun aMultiWordTriggerDoesNotFireInsideALongerWord() {
        // The prototype's stand-in fired here, because it branched on whether the
        // trigger contained a space. This is that test, inverted.
        val result = dictionary("design off the layout", dict("sign off", "Goodbye."))

        assertEquals(listOf(dictated("design off the layout")), result.segments)
    }

    @Test
    fun aMultiWordTriggerMatchesAtAnyPositionAndAcrossPunctuation() {
        assertEquals("please Goodbye. the draft", dictionary("please sign off the draft", dict("sign off", "Goodbye.")).text)
        // The hyphen separates tokens, so the trigger matches this spelling too.
        assertEquals("please Goodbye.", dictionary("please sign-off", dict("sign off", "Goodbye.")).text)
        // Punctuation *outside* the matched range is untouched, so the brackets stay.
        assertEquals("(Goodbye.)", dictionary("(sign off)", dict("sign off", "Goodbye.")).text)
    }

    @Test
    fun aMatchMaySpanAParagraphBreak() {
        val result = dictionary("i will sign\noff now", dict("sign off", "Goodbye."))

        assertEquals("i will Goodbye. now", result.text)
    }

    // ── the replaced range ────────────────────────────────────────────────────────

    @Test
    fun theReplacedRangeRunsFromTheFirstTokensStartToTheLastTokensEnd() {
        // Irregular spacing *inside* the match disappears with the match; the text
        // outside the range survives verbatim, including its own odd spacing.
        val result = dictionary("  please   SIGN  off  now", dict("sign off", "Goodbye."))

        assertEquals("  please   Goodbye.  now", result.text)
        assertEquals(
            listOf(dictated("  please   "), authored("Goodbye."), dictated("  now")),
            result.segments,
        )
    }

    @Test
    fun everyOccurrenceIsReplacedAndTheScanResumesAfterEachMatch() {
        assertEquals("Kubernetis and Kubernetis", dictionary("sig and sig", dict("sig", "Kubernetis")).text)
    }

    @Test
    fun aShorterTriggerDoesNotFireInsideAMatchAlreadyChosen() {
        // `sign` cannot take the `sign` of `sign off`, because the scan resumes after
        // the whole match rather than inside it.
        val result = dictionary("please sign off", dict("sign off", "Goodbye."), dict("sign", "bye"))

        assertEquals("please Goodbye.", result.text)
    }

    @Test
    fun nothingInsideAChosenMatchIsMatchedAgain() {
        // Both entries match at position 0 and `sign off` is longer, so it wins the
        // position outright. `off` must then never be reached — the scan resumes *after*
        // the range it replaced, so a trigger matching inside that range stays unused.
        // If the scan advanced one token instead of past the match, this would read
        // "Goodbye.Goodbye." with the second body appended after the first.
        val result = dictionary("sign off", dict("sign off", "Goodbye."), dict("off", "Later"))

        assertEquals("Goodbye.", result.text)
        assertEquals(listOf(authored("Goodbye.")), result.segments)
    }

    @Test
    fun aMatchLaterInTheInputIsUnaffectedByAnEarlierOne() {
        // The same rule with the second match outside the first one's tokens: the scan
        // skips the tokens it consumed and resumes on the next position.
        val result = dictionary(
            "sig then sign off",
            dict("sign off", "Goodbye."),
            dict("sig", "Kubernetis"),
        )

        assertEquals("Kubernetis then Goodbye.", result.text)
    }

    // ── stored trigger spelling is not load-bearing ────────────────────────────────

    @Test
    fun punctuationAndCaseInAStoredTriggerAreNotLoadBearing() {
        assertEquals("please Goodbye.", dictionary("please sign off", dict("  Sign-Off!  ", "Goodbye.")).text)
        assertEquals("please Goodbye.", dictionary("please SIGN OFF", dict("sign off", "Goodbye.")).text)
        // …and the reverse direction: the spoken text's spelling is equally free.
        assertEquals("please Goodbye.", dictionary("please SiGn OfF", dict("sign off", "Goodbye.")).text)
    }

    // ── no accent folding ─────────────────────────────────────────────────────────

    @Test
    fun anUnaccentedTriggerMatchesTheUnaccentedWord() {
        // The everyday case, stated so the tests below are about accents and not about
        // the matcher simply failing to match.
        assertEquals(
            listOf(dictated("i had a "), authored("coffee")),
            dictionary("i had a cafe", dict("cafe", "coffee")).segments,
        )
    }

    @Test
    fun cafeDoesNotMatchCafe() {
        // An accented trigger does not reach an unaccented word. Folding accents would
        // rewrite "cafe" here; not folding leaves it alone.
        assertEquals(listOf(dictated("i had a cafe")), dictionary("i had a cafe", dict("café", "coffee")).segments)
    }

    @Test
    fun anAccentedTriggerDoesNotMatchTheUnaccentedWordEither() {
        // The other direction, so the rule is about accents rather than about which
        // side happens to be spelled with one.
        assertEquals(listOf(dictated("i had a café")), dictionary("i had a café", dict("cafe", "coffee")).segments)
    }

    @Test
    fun anAccentedTriggerMatchesTheSameWordWhateverItsCase() {
        assertEquals("i had a café.", dictionary("i had a CAFÉ", dict("café", "café.")).text)
    }

    @Test
    fun resumeDoesNotMatchResumeBecauseFoldingWouldMergeThem() {
        // If accents were folded, this would rewrite a word the user did not say.
        assertEquals(listOf(dictated("my résumé")), dictionary("my résumé", dict("resume", "CV")).segments)
    }

    // ── entry-list order is never consulted ───────────────────────────────────────

    @Test
    fun permutingTheEntryListCannotChangeTheResult() {
        val entries = listOf(
            dict("sign off", "Goodbye."),
            dict("sign", "bye", EntryScope.App(whatsapp)),
            dict("sig", "ok"),
            dict("iphone 15", "iPhone"),
            snippet("brb", "Be right back."),
        )
        val input = "i will sign off and sig on my iphone 15, brb"

        val permutations = listOf(
            entries,
            entries.reversed(),
            listOf(entries[2], entries[4], entries[0], entries[3], entries[1]),
            listOf(entries[1], entries[3], entries[2], entries[4], entries[0]),
            entries.sortedBy { it.trigger },
            entries.sortedByDescending { it.trigger },
        )
        // Both stages, so the list exercises the dictionary's app/global collision and
        // the snippet pass over its output.
        val results = permutations.map { list ->
            val matcher = EntryMatcher(list)
            matcher.applySnippets(matcher.applyDictionary(input, whatsapp), whatsapp)
        }

        results.forEach { assertEquals(results.first().segments, it.segments) }
        // And the answer is the one the rules give, so the permutation test is not
        // asserting that every permutation is equally wrong.
        assertEquals(
            "i will bye off and ok on my iPhone, Be right back.",
            results.first().text,
        )
    }

    // ── origin and the pipeline order between the two stages ──────────────────────

    @Test
    fun aDictionaryBodyIsAuthoredSoStyleCannotRepairItsCapitalisation() {
        // `ios → iphone` stays lowercase under Formal, because the body is the user's
        // own spelling and stage 8 may not write to authored text.
        val result = dictionary("ios is great", dict("ios", "iphone"))

        assertEquals(listOf(authored("iphone"), dictated(" is great")), result.segments)
    }

    @Test
    fun aStage6BodyIsScannableByStage7() {
        // The dictionary runs, then the snippet fires on its *output*.
        val afterDictionary = match(dict("em", "sig")).applyDictionary("em will go", whatsapp)
        assertEquals("sig will go", afterDictionary.text)

        val afterSnippets = match(snippet("sig", "Kubernetis")).applySnippets(afterDictionary, whatsapp)

        assertEquals("Kubernetis will go", afterSnippets.text)
    }

    @Test
    fun aMatchSpanningAStage6BoundaryBecomesWhollyAuthored() {
        val afterDictionary = match(dict("em", "I")).applyDictionary("em will go", whatsapp)
        // The boundary is real: an authored run followed by a dictated one.
        assertEquals(listOf(authored("I"), dictated(" will go")), afterDictionary.segments)

        val afterSnippets = match(snippet("i will", "I will send the draft.")).applySnippets(afterDictionary, whatsapp)

        // The match starts inside the stage-6 body and ends in dictated text. It is
        // authored whole — there is no partly-dictated remnant of it.
        assertEquals(
            listOf(authored("I will send the draft."), dictated(" go")),
            afterSnippets.segments,
        )
    }

    @Test
    fun stage7InsertionsAreNotRescannedByStage7() {
        // `Kubernetis` is a live trigger in the same stage as the body that contains
        // it. One pass over the input does not see what stage 7 itself inserted.
        val result = snippets(
            RefinedText.dictated("ok sig now"),
            snippet("sig", "Kubernetis"),
            snippet("Kubernetis", "Kubernetes"),
        )

        assertEquals("ok Kubernetis now", result.text)
    }

    @Test
    fun aSnippetBodyIsNeverDictionaryCorrected() {
        // Stage 6 already ran. `sig → kubernetis` stays uncorrected, because re-running
        // stage 6 after stage 7 would reopen the pipeline order.
        val afterDictionary = match(dict("kubernetis", "Kubernetes")).applyDictionary("sig", whatsapp)
        assertEquals(listOf(dictated("sig")), afterDictionary.segments)

        val result = match(dict("kubernetis", "Kubernetes"), snippet("sig", "kubernetis"))
            .applySnippets(afterDictionary, whatsapp)

        assertEquals(listOf(authored("kubernetis")), result.segments)
    }

    @Test
    fun aDictionaryEntryWhoseBodyIsATriggerDoesExpand() {
        // The pipeline order working as intended: `wrong → sig` yields the snippet.
        val afterDictionary = match(dict("em", "sig")).applyDictionary("em", whatsapp)
        val result = match(snippet("sig", "Kubernetis")).applySnippets(afterDictionary, whatsapp)

        assertEquals("Kubernetis", result.text)
    }

    @Test
    fun theSameTriggerMayBeADictionaryEntryAndASnippetInOneScope() {
        // Cross-kind overlap is not a conflict: the dictionary runs, then the snippet
        // fires on its output — here because the dictionary body *is* the trigger.
        val matcher = match(dict("kubernetis", "sig"), snippet("sig", "Kubernetis"))
        val afterDictionary = matcher.applyDictionary("kubernetis now", whatsapp)
        val afterSnippets = matcher.applySnippets(afterDictionary, whatsapp)

        assertEquals("Kubernetis now", afterSnippets.text)
        assertEquals(listOf(authored("Kubernetis"), dictated(" now")), afterSnippets.segments)
    }

    @Test
    fun aStageOnlySeesItsOwnKind() {
        val matcher = match(dict("sig", "K8s"), snippet("sig", "Kubernetis"))

        // Stage 6 ignores the snippet, stage 7 ignores the dictionary entry.
        assertEquals("K8s now", matcher.applyDictionary("sig now", whatsapp).text)
        assertEquals(
            listOf(authored("Kubernetis"), dictated(" now")),
            matcher.applySnippets(RefinedText.dictated("sig now"), whatsapp).segments,
        )
    }

    // ── edges ──────────────────────────────────────────────────────────────────────

    @Test
    fun noApplicableEntryLeavesTheTextExactlyAsItWas() {
        val result = dictionary("nothing to do here", dict("sig", "Kubernetis"))

        assertEquals(listOf(dictated("nothing to do here")), result.segments)
    }

    @Test
    fun emptyInputStaysEmpty() {
        val result = dictionary("", dict("sig", "Kubernetis"))

        assertEquals(emptyList<OriginSegment>(), result.segments)
        assertTrue(result.isEmpty)
    }

    @Test
    fun aReplacementMayBeAnEmptyBody() {
        val result = dictionary("please sig now", dict("sig", ""))

        assertEquals("please  now", result.text)
    }

    @Test
    fun aSnippetMayBeAMultilineBlockAndItsWhitespaceIsAuthoredVerbatim() {
        val result = snippets(
            RefinedText.dictated("please review this. brb"),
            snippet("brb", "Best regards,\n\nMitun\nOpenFlow,"),
        )

        assertEquals(
            listOf(
                dictated("please review this. "),
                authored("Best regards,\n\nMitun\nOpenFlow,"),
            ),
            result.segments,
        )
        assertEquals("please review this. Best regards,\n\nMitun\nOpenFlow,", result.text)
    }
}