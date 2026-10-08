package dev.openflow.dictation.core.stt

/**
 * What a provider charges for audio: integer **micros of USD per second**.
 *
 * V1 is USD-only. A provider billing in another currency converts at its own
 * boundary or declares `pricing = null`; OpenFlow computes no exchange rate.
 *
 * ### The distinction this type exists to protect
 *
 * `Pricing(0)` means *known free* — every local provider declares it.
 * `capabilities.pricing == null` means *cannot estimate*, and is a different
 * answer, not a missing value.
 *
 * Collapsing them is the one mistake in this package with a money
 * consequence. A cloud adapter that reports `null` "because we don't know" is
 * correct and honest; an adapter that coerces it to `0` makes a metered API
 * look free and then passes every cost-ceiling check while billing real money.
 * The ceiling is compared against a worst case derived from this rate
 * (ADR-0004 rule 4), so a wrong rate silently moves the ceiling rather than
 * tripping it.
 *
 * This replaces an `estimatedCostAvailability` boolean that sat beside the
 * amount. Nullability already says "cannot estimate", so carrying both would
 * have recreated the two-sources-of-truth defect nullability exists to remove
 * (ticket 22).
 */
data class Pricing(val microsUsdPerSecond: Long) {

    init {
        require(microsUsdPerSecond >= 0) {
            "microsUsdPerSecond must not be negative, was $microsUsdPerSecond. " +
                "A rate is never negative; 0 is the honest value for a free provider, " +
                "and a provider that cannot estimate declares pricing = null."
        }
    }

    /**
     * The most this request could cost: the declared rate against the whole of
     * the provider's own [maxAudioDurationSeconds][Capabilities.maxAudioDurationSeconds].
     *
     * A bound, not an estimate, and derived from the two *declared* inputs
     * rather than supplied by the adapter. An adapter-supplied cost figure is
     * something the adapter could overstate, and a ceiling that can be exceeded
     * after the router has committed is not a ceiling (ADR-0004 rule 4).
     *
     * Callers reaching here have already established that the rate is known:
     * the `null` case cannot reach this method, because it has no rate to
     * multiply. That is the whole point of the type being nullable at the
     * capability rather than a sentinel value here.
     */
    fun worstCaseMicrosUsd(maxAudioDurationSeconds: Int): Long {
        require(maxAudioDurationSeconds > 0) {
            "maxAudioDurationSeconds must be positive, was $maxAudioDurationSeconds"
        }
        return microsUsdPerSecond * maxAudioDurationSeconds
    }

    companion object {
        /**
         * Known free. Named so that "free" and "unknown" cannot be confused at
         * a call site: a local provider writes `pricing = Pricing.FREE`, which
         * reads differently from `pricing = null` even though both are one
         * token shorter than the long form.
         */
        val FREE = Pricing(0L)
    }
}