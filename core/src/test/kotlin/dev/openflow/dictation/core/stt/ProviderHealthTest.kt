package dev.openflow.dictation.core.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one relationship ADR-0001 declares between [ProviderState] and
 * [ProviderHealth], and the two vocabularies' refusal to merge.
 *
 * Ticket 22 split these deliberately and ADR-0001's Consequences names the
 * failure it anticipates: a contributor reading `READY` beside `HEALTHY` will
 * be tempted to merge the types, and they must not merge. A merge would not be
 * a simplification, because `CLOSED` is something the app did and has no health
 * counterpart, so a single enum would force every consumer to re-decide which
 * question it asked.
 */
class ProviderHealthTest {

    @Test
    fun readyIsInconsistentWithAMissingModel() {
        assertFalse(
            "a provider that finished loading cannot also be missing its model, " +
                "so this pair must be rejected rather than reported",
            ProviderState.READY.isConsistentWith(ProviderHealth.MODEL_MISSING),
        )
    }

    @Test
    fun readyIsConsistentWithEveryOtherVerdict() {
        listOf(ProviderHealth.HEALTHY, ProviderHealth.DEGRADED, ProviderHealth.UNAVAILABLE)
            .forEach { health ->
                assertTrue(
                    "READY with $health is a real condition -- a loaded engine that " +
                        "stopped serving, or one serving badly -- and rejecting it " +
                        "would leave the router no way to describe a degraded engine " +
                        "that is nonetheless loaded",
                    ProviderState.READY.isConsistentWith(health),
                )
            }
    }

    @Test
    fun theInvariantConstrainsExactlyOneCellOfTheGrid() {
        // Fifteen of the sixteen pairs are legal. Written out rather than left
        // implicit because the tempting "fix" is to add a second rule -- READY
        // implies HEALTHY, say -- which would forbid the DEGRADED and UNAVAILABLE
        // cases ADR-0004 rule 5 needs in order to rank a provider rather than
        // silently drop it.
        val legal = mutableListOf<Pair<ProviderState, ProviderHealth>>()
        ProviderState.entries.forEach { state ->
            ProviderHealth.entries.forEach { health ->
                if (state.isConsistentWith(health)) legal += state to health
            }
        }

        assertEquals(
            "ADR-0001 declares one forbidden pair out of sixteen. More than one " +
                "means a second rule crept in.",
            15,
            legal.size,
        )
        assertFalse(ProviderState.READY to ProviderHealth.MODEL_MISSING in legal)
    }

    @Test
    fun notPreparedMayReportAMissingModel() {
        // The other direction of the pairing, and the one ADR-0010's Model
        // Delivery makes the common case: a fresh install has no model and no
        // error, which is what MODEL_MISSING reports from the first launch.
        assertTrue(
            ProviderState.NOT_PREPARED.isConsistentWith(ProviderHealth.MODEL_MISSING),
        )
    }

    @Test
    fun closedHasNoHealthCounterpartAndHealthHasNoClosedValue() {
        val lifecycleNames = ProviderState.entries.map { it.name }.toSet()
        val healthNames = ProviderHealth.entries.map { it.name }.toSet()

        assertTrue(
            "CLOSED is something the app did, not something an engine reported, " +
                "so it must have no health counterpart. Found $healthNames",
            "CLOSED" !in healthNames,
        )
        assertEquals(
            "the two vocabularies must share no value name. Any overlap is the " +
                "merge ADR-0001 forbids, and it starts with one value that looks " +
                "reasonable in isolation. Overlap was " +
                (lifecycleNames intersect healthNames),
            emptySet<String>(),
            lifecycleNames intersect healthNames,
        )
    }

    @Test
    fun degradedIsUsableRatherThanOnTheWayToUnavailable() {
        // Not a behavioural test -- nothing in the type distinguishes these -- but
        // a pin on the fact that the distinction is carried by the ADR and not by
        // the enum, so that it cannot be quietly lost. With one real provider and
        // the default privacyMode of local-only, an engine that DEGRADED meant as
        // "on the way to UNAVAILABLE" would leave the user unable to dictate at
        // all, because ADR-0004 admits DEGRADED entries under DEGRADED_LAST_RESORT
        // but never admits UNAVAILABLE ones.
        assertEquals(4, ProviderHealth.entries.size)
        assertTrue(ProviderHealth.DEGRADED in ProviderHealth.entries)
        assertTrue(ProviderHealth.UNAVAILABLE in ProviderHealth.entries)
    }

    @Test
    fun healthHasExactlyTheFourValuesTicket22Settled() {
        // Named here rather than left to the enum's own definition because
        // ADR-0004's rule 5 filters on these four by name: UNAVAILABLE and
        // MODEL_MISSING skip, DEGRADED ranks last, HEALTHY is the ordinary case.
        // A fifth value would have no rule to act on it, and a removal would
        // leave the router referencing something that cannot exist.
        assertEquals(
            listOf("HEALTHY", "DEGRADED", "UNAVAILABLE", "MODEL_MISSING"),
            ProviderHealth.entries.map { it.name },
        )
        assertEquals(
            listOf("NOT_PREPARED", "PREPARING", "READY", "CLOSED"),
            ProviderState.entries.map { it.name },
        )
    }
}