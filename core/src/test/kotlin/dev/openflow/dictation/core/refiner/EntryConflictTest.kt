package dev.openflow.dictation.core.refiner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The two settings-side checks the prototype built ahead of the matcher, tested
 * against the matcher they protect.
 *
 * Both are refusals at save rather than tie-breaks at match time: a list cannot show
 * the user which of two entries would win, so the only moment the ambiguity is
 * resolvable is the moment the entry is created.
 */
class EntryConflictTest {

    private fun dict(trigger: String, body: String = "Kubernetis", scope: EntryScope = EntryScope.EveryApp) =
        RefinerEntry(RefinerEntryKind.DICTIONARY, trigger, body, scope)

    private fun snippet(trigger: String, body: String = "Be right back.", scope: EntryScope = EntryScope.EveryApp) =
        RefinerEntry(RefinerEntryKind.SNIPPET, trigger, body, scope)

    // ── a trigger with no word in it ──────────────────────────────────────────────

    @Test
    fun aTriggerWithNoWordIsRefused() {
        assertEquals(EntryConflict.NoWordInTrigger, checkNewEntry(RefinerEntryKind.DICTIONARY, "!!!", emptyList()))
        assertEquals(EntryConflict.NoWordInTrigger, checkNewEntry(RefinerEntryKind.DICTIONARY, "   ", emptyList()))
        assertEquals(EntryConflict.NoWordInTrigger, checkNewEntry(RefinerEntryKind.SNIPPET, "-", emptyList()))
        // And it cannot be reached by another route either: an existing list cannot
        // hold a no-word entry, because constructing one is refused, so the check's
        // ordering against duplication is unobservable rather than merely untested.
        assertEquals(EntryConflict.NoWordInTrigger, checkNewEntry(RefinerEntryKind.DICTIONARY, "?", listOf(dict("sig"))))
    }

    @Test
    fun aTriggerWithNoWordCannotBeStoredAtAll() {
        // The check is not decoration on a path that also works: the entry's own
        // invariant refuses to construct one, so a caller bypassing `checkNewEntry`
        // cannot smuggle an entry that can never match into a matcher's list.
        try {
            dict("!!!")
            fail("expected a trigger with no word to be refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("no word"))
        }
    }

    @Test
    fun aDigitIsAWordAndSoAMatchableTrigger() {
        assertNull(checkNewEntry(RefinerEntryKind.DICTIONARY, "24", emptyList()))
    }

    // ── two triggers colliding in one scope ───────────────────────────────────────

    @Test
    fun theSameTriggerTwiceInOneScopeIsRefusedAndNamesTheExistingEntry() {
        val existing = dict("sign off", "Goodbye.")
        val conflict = checkNewEntry(RefinerEntryKind.DICTIONARY, "sign off", listOf(existing))

        assertTrue(conflict is EntryConflict.DuplicateTrigger)
        // The user's next question is "which one?", so the entry that already claims
        // the trigger has to come back with the refusal.
        assertEquals(existing, (conflict as EntryConflict.DuplicateTrigger).existing)
        assertEquals("sign off", conflict.existing.trigger)
        assertEquals("Goodbye.", conflict.existing.body)
        // The rendered message is what a settings surface would show, so the existing
        // entry has to be identifiable from it and not only from the type.
        assertTrue(conflict.message.contains("“sign off”"))
        assertTrue(conflict.message.contains("“Goodbye.”"))
        assertTrue(conflict.message.contains("every app"))
    }

    @Test
    fun triggersAreComparedAsTokenSequencesSoPunctuationIsNotADistinctTrigger() {
        val existing = dict("sign off", "Goodbye.")
        listOf("sign-off", "Sign Off", "SIGN  OFF", " sign off! ", "sign\noff").forEach { spelling ->
            val conflict = checkNewEntry(RefinerEntryKind.DICTIONARY, spelling, listOf(existing))
            assertTrue(
                "“$spelling” should collide with “sign off”",
                conflict is EntryConflict.DuplicateTrigger,
            )
        }
    }

    @Test
    fun aDifferentTriggerInOneScopeIsNotADuplicate() {
        assertNull(checkNewEntry(RefinerEntryKind.DICTIONARY, "sign", listOf(dict("sign off", "Goodbye."))))
        // A trigger that merely *starts with* an existing one is a different trigger.
        assertNull(checkNewEntry(RefinerEntryKind.DICTIONARY, "sign off now", listOf(dict("sign off", "Goodbye."))))
    }

    @Test
    fun theSameTriggerInDifferentScopesIsAllowed() {
        val global = dict("sig", "ok")
        val whatsapp = dict("sig", "bye", EntryScope.App("com.whatsapp"))

        assertNull(checkNewEntry(RefinerEntryKind.DICTIONARY, "sig", listOf(global), EntryScope.App("com.slack")))
        // Same scope, so this one is refused.
        assertEquals(
            EntryConflict.DuplicateTrigger(whatsapp),
            checkNewEntry(RefinerEntryKind.DICTIONARY, "sig", listOf(global, whatsapp), EntryScope.App("com.whatsapp")),
        )
    }

    @Test
    fun theSameTriggerAsBothKindsInOneScopeIsAllowed() {
        // The stages are ordered, so the dictionary runs and the snippet then fires on
        // its output. Cross-kind overlap is not a conflict at all.
        assertNull(checkNewEntry(RefinerEntryKind.SNIPPET, "sig", listOf(dict("sig", "Kubernetis"))))
        assertNull(checkNewEntry(RefinerEntryKind.DICTIONARY, "sig", listOf(snippet("sig"))))
    }

    @Test
    fun theCheckAndTheMatcherAgreeOnWhatADuplicateIs() {
        // The check and the matcher derive the trigger key the same way, so nothing can
        // pass save and then turn out to be something the matcher treats as different.
        val existing = dict("Sign-Off!")
        assertNull(checkNewEntry(RefinerEntryKind.SNIPPET, "sign off", listOf(existing)))
        // Both spellings match the same input, which is why they cannot both be stored.
        val text = "please sign off"
        val viaStoredSpelling = EntryMatcher(listOf(existing)).applyDictionary(text, "com.whatsapp")
        val viaNewSpelling = EntryMatcher(listOf(dict("sign off"))).applyDictionary(text, "com.whatsapp")

        assertEquals(viaNewSpelling.segments, viaStoredSpelling.segments)
    }
}