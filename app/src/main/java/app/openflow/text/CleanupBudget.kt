package app.openflow.text

import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cleanup budget + honest fallback (T15).
 * Local rules are <50ms for 500 words; the budget only bounds a cloud brain hop
 * or an unexpected pipeline stall. On timeout the bubble inserts the untouched
 * transcript with a notice — critical path never blocked, no silent drop.
 */
object CleanupBudget {

    const val POLISH_MS = 5_000L

    /** Raw-text fallback result. Null when there is nothing to insert. */
    fun fallback(raw: String): CleanupResult? =
        raw.trim().takeIf { it.isNotEmpty() }?.let { CleanupResult(raw = it, clean = it) }

    /** Run [block] or return null when the budget expires. Never throws on timeout. */
    suspend fun <T> within(ms: Long = POLISH_MS, block: suspend () -> T): T? =
        withTimeoutOrNull(ms) { block() }
}
