package dev.openflow.dictation.core.stt

/**
 * The readiness verdict [SpeechProvider.health] returns: whether this provider
 * can serve a Dictation **right now**, and if not, why.
 *
 * A separate vocabulary from [ProviderState], not a fifth lifecycle value. A
 * future contributor reading `ProviderState.READY` next to
 * `ProviderHealth.HEALTHY` will be tempted to merge the two types, and they
 * must not merge: `CLOSED` is something the app did, not something the engine
 * reported, so a single enum cannot express both questions without every
 * consumer re-deciding which one it asked.
 *
 * The four values carry three distinct remediations — do nothing, retry, or
 * download — which is why they are four values and not one boolean. The router
 * acts on that difference directly: `UNAVAILABLE` and `MODEL_MISSING` are
 * skipped, `DEGRADED` is eligible but ranked last (ADR-0004 rule 5).
 */
enum class ProviderHealth {

    /** Can serve now. */
    HEALTHY,

    /**
     * Usable but worse: a model is on disk but degraded, the engine is loaded
     * under contention, or the last failure left it limping.
     *
     * Eligible, ranked last, and never on the way to
     * [UNAVAILABLE] — with one real provider and the default
     * `privacyMode = local-only`, sinking a mildly degraded engine leaves the
     * user unable to dictate at all, so ADR-0004 admits `DEGRADED` entries
     * under `DEGRADED_LAST_RESORT` rather than returning nothing.
     *
     * Slowness on this device is one of the causes folded in here (ticket 31),
     * and it is narrowed in by the router from measured Final Latency rather
     * than reported by the adapter: a capability is a static claim about a
     * model, slowness is an observation about this device over time, and
     * conflating them would make a slow device indistinguishable from a
     * missing model.
     */
    DEGRADED,

    /**
     * Transient failure — a retry may work. The engine is present but not
     * serving now.
     */
    UNAVAILABLE,

    /**
     * Weights absent or unloadable. Needs a **download**, never a retry.
     *
     * Its own value rather than a flavour of [UNAVAILABLE] precisely because
     * the remedies differ, and because ADR-0010's Model Delivery makes the
     * streaming ASR model a first-launch download: a fresh install has no
     * model and no error, which is the state this value reports from the first
     * launch onward. See [FailureReason.OfflineModelMissing] for the same
     * condition observed mid-call.
     */
    MODEL_MISSING,
}

/**
 * The one relationship ADR-0001 declares between [ProviderState] and
 * [ProviderHealth]: `READY` may not be reported alongside `MODEL_MISSING`.
 *
 * A provider that has finished loading cannot simultaneously be missing its
 * model, so the pair is a contradiction rather than a degraded state, and
 * leaving it to judgement would let an adapter report whichever it liked and
 * push the router's download-prompt-vs-retry decision onto a bug.
 *
 * Every other combination is allowed, including the ones that look odd:
 * `NOT_PREPARED` with `HEALTHY` is an instance the router may pick and prepare,
 * and `READY` with `UNAVAILABLE` is a loaded engine that stopped serving. The
 * invariant constrains one cell of a 4x4 grid, not the shape of the enums —
 * which is the point of keeping them apart.
 *
 * The Contract Test asserts this over observed provider state rather than over
 * this function, so a provider cannot satisfy it by calling something.
 */
fun ProviderState.isConsistentWith(health: ProviderHealth): Boolean =
    this != ProviderState.READY || health != ProviderHealth.MODEL_MISSING