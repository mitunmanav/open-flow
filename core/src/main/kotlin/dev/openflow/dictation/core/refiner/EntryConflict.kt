package dev.openflow.dictation.core.refiner

/**
 * Why a new entry may not be saved. Both cases are refusals at save rather than
 * tie-breaks at match time, and the reason is the same for each: a list cannot show
 * the user which of two entries would win, so the only moment the ambiguity is
 * resolvable is the moment it is created — and refusing it deletes an ambiguity from
 * the runtime contract instead of encoding one.
 */
sealed interface EntryConflict {

    /**
     * A short explanation naming what already exists, for a settings surface to show
     * under the trigger field. The wording is this package's, not the surface's: it
     * exists so "the existing entry is named" is something a test can check rather
     * than something a review has to trust.
     */
    val message: String

    /**
     * The trigger holds no word, so it can never match anything said out loud. It is
     * refused rather than stored, because a stored entry that does nothing is
     * indistinguishable from a bug.
     */
    data object NoWordInTrigger : EntryConflict {
        override val message: String =
            "That has no words in it, so it would never match anything said out loud."
    }

    /**
     * Another entry of the same kind and scope already claims this trigger. [existing]
     * is that entry, named because the user's next question is "which one?" and
     * "nothing, yet" is an unhelpful answer.
     *
     * Two triggers are compared as token sequences, so `sign-off` collides with
     * `sign off`: the check and the matcher derive the key the same way, which is what
     * keeps them from disagreeing.
     */
    data class DuplicateTrigger(val existing: RefinerEntry) : EntryConflict {
        override val message: String =
            "“${existing.trigger}” is already an entry for ${existing.scope}, " +
                "becoming “${existing.body}”. One trigger, one replacement."
    }
}

/**
 * Whether a new entry may be saved against [existing], or the conflict that stops it.
 *
 * Returns null when it may be saved. Two things are deliberately *not* conflicts:
 *
 * - **The same trigger in different scopes.** It is allowed, and specificity resolves
 *   it — the app's own entry wins, including against a longer global one.
 * - **The same trigger as a dictionary entry and a snippet in one scope.** The stages
 *   are ordered, so the dictionary runs first and the snippet then fires on its output.
 *   Cross-kind overlap is not a conflict at all.
 *
 * [existing] must already hold every saved entry, and must exclude the entry being
 * edited — a rename that collides with itself would refuse every edit.
 *
 * A trigger that has passed this check can still be refused by [RefinerEntry]'s own
 * invariant, which exists as a backstop for code paths that build an entry without
 * coming through the surface.
 */
fun checkNewEntry(
    kind: RefinerEntryKind,
    trigger: String,
    existing: List<RefinerEntry>,
    scope: EntryScope = EntryScope.EveryApp,
): EntryConflict? {
    val key = RefinerTokens.triggerKey(trigger)
    if (key.isEmpty()) return EntryConflict.NoWordInTrigger
    val clash = existing.firstOrNull {
        it.kind == kind && it.scope == scope && it.triggerKey == key
    }
    return clash?.let { EntryConflict.DuplicateTrigger(it) }
}