# Specify ProviderHealth and the cost-reporting shape

Type: grilling
Status: resolved
Blocked by: none

## Question

Two contract gaps surfaced while documenting the provider seam (ticket 21). Both are defects in ADR-0001/0004, not documentation problems, so they get their own ticket.

**Health has two colliding vocabularies and no declared type.** ADR-0001 defines `ProviderState` = `NOT_PREPARED/PREPARING/READY/CLOSED` and lists `health()` as a method but never gives it a return type. ADR-0004 rule 4 then filters on `NOT_READY`/`UNAVAILABLE`/`DEGRADED` — none of which are `ProviderState` values — so `READY` (lifecycle) and `NOT_READY` (health) read like two values of one enum when they are different concepts. Neither set can express what `docs/providers/sherpa-onnx.md` already requires: a **"model missing"** health state, which `FailureReason.OfflineModelMissing` covers during a call but not in the pre-call filter the router runs. Decide: the `ProviderHealth` type and its state set (including a `HEALTHY` counterpart and a model-missing case); whether health and `ProviderState` stay separate types or merge; the `health()` return type under ADR-0004's "cheap local truth, no active probe" constraint; and the `MODEL_MISSING` vs `FailureReason.OfflineModelMissing` boundary.

**Cost reporting is a flag where an amount is needed.** `estimatedCostAvailability` is a boolean capability, but ADR-0004 rule 3 compares an estimated *per-request cost* against `costCeiling`. A boolean cannot express an amount. Decide the cost-reporting shape — per-request estimate, currency, and how an unknown/unavailable estimate is handled against a ceiling — and how it relates to the boolean capability.

Then revise ADR-0001 and ADR-0004 to match, and check the router's rule-4 filter and `ProviderState` in `GLOSSARY.md` against whatever is decided.

## Answer

Both gaps were real contract defects, not documentation omissions: ADR-0001 declared a `health()` with no return type and a boolean cost capability, and ADR-0004 filtered on three values that existed in neither type. Both ADRs amended in place; `docs/providers/sherpa-onnx.md` reworded, since it had described a "model missing **degraded** state" that the settled set splits in two.

**Health: two types, four values.** `ProviderHealth` is a flat enum — `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, `MODEL_MISSING` — kept separate from `ProviderState` and never sharing its value set. Declared invariant: `ProviderState.READY` implies health ∈ {`HEALTHY`, `DEGRADED`, `UNAVAILABLE`}, so `MODEL_MISSING` is necessarily `NOT_PREPARED`, and no health value mirrors a lifecycle value. `MODEL_MISSING` is its own value rather than a flavour of `UNAVAILABLE` because its remediation is a download prompt, not a retry, and ADR-0001's thesis is that distinct actions get distinct values.

**`health()` is a plain synchronous method**, read from the adapter's cached local state — no probe, no network call, and deliberately no `StateFlow`. The tie-breaker: V1's only consumer is the router reading a snapshot at `PREPARING`, and a health stream invites exactly the automatic health-based rerouting ADR-0004 defers. `StateFlow<ProviderState>` stays the only reactive surface in V1, and it reports lifecycle the app itself drives. Only the adapter authors health; the router may narrow a verdict but never upgrade one, which keeps `choose()` pure.

**`MODEL_MISSING` vs `OfflineModelMissing`: one condition at two times.** The health value is the pre-call verdict, the `FailureReason` the mid-call outcome, joined by a Contract-Test invariant that a provider reporting `MODEL_MISSING` must fail `transcribe()` with `OfflineModelMissing` and nothing generic. The sub-question that mattered: `OfflineModelMissing` is `recoverable = true`. `recoverable` means *the dictation can still complete by another route*, not *this call will succeed* — which is precisely how ADR-0004 rule 5 uses it. Had it stayed false, a missing model would have killed the whole dictation while another eligible provider sat idle. That reading of `recoverable` is now written into ADR-0001, since nothing stated it before.

**Cost: an amount, not a flag.** `estimatedCostAvailability` is deleted and replaced by `pricing: Pricing?` carrying `microsUsdPerSecond: Long`. Nullness already expresses "cannot estimate", so keeping the boolean alongside would have recreated the two-sources-of-truth defect this ticket exists to remove. Nullability semantics: **known-free is `0`, unknown is `null`** — local providers declare `0`, and defaulting null to zero would make a paid API look free and pass every ceiling check. Integer micros of USD, USD-only in V1, with a non-USD provider converting at its own boundary or declaring null.

**The ceiling compares a bound, not an estimate.** The router runs at `PREPARING`, before a sample exists, so it compares `pricing.microsUsdPerSecond × maxAudioDurationSeconds` — a ceiling that can be exceeded after the router commits is not a ceiling. `costCeilingMicrosUsd: Long?` is per-dictation, `null` meaning no ceiling (an absent ceiling is not a zero ceiling), explicitly not a monthly budget: a budget needs metering and a period, neither of which V1 has.

**Pricing knowledge stays declarative.** The declared rate lives in capabilities and the router does the arithmetic, rather than an adapter-supplied `estimatedCost(request)` number. A guarantee the adapter can overstate is advisory, which is the defect being fixed; a declared rate is also Contract-Testable (non-negative, local providers `0`).

**ADR-0004's rules gained an order, because "which rule excluded this provider" is the user's explanation.** Preference (ranking override, never above a hard exclusion) → privacy/offline → capability match → cost ceiling → health. Consent first because it is the user's explicit instruction; health last among exclusions because it is the most volatile and least authoritative signal.

**`DEGRADED` ranks last instead of sinking.** The old "sinks" was ambiguous, and reading it as *excluded* was a live hazard: with one real provider and the default `privacyMode = local-only`, excluding a mildly degraded engine leaves zero candidates and the user cannot dictate at all. So `DEGRADED` is eligible but ranked last, `UNAVAILABLE`/`MODEL_MISSING` skip, and health never empties the candidate set — `DEGRADED` entries are admitted under `DEGRADED_LAST_RESORT` when skipping would leave nothing.

**Empty candidate sets are still decisions.** `RoutingDecision` returns `chosen = null` with `blockedBy` (first rule that emptied the set) and `rejected` (each provider with its excluding rule). Without this the bubble can only say "no provider available", when the truth may be no model installed, nothing allowed offline, or over the ceiling — three failures with three different fixes.

**Unknown cost renders `unknown`, in mono.** Never `$0.00`, never blank: a blank reads as free to exactly the privacy-conscious user this app is for, and would quietly defeat the ceiling. The decision log records `estimatedCostMicrosUsd`, `worstCaseMicrosUsd`, `ceilingAppliedMicrosUsd`, and the excluding rule, so "why this provider?" stays answerable offline. No user-facing cost control ships in V1 — no billable provider means a ceiling slider controls nothing; the field exists so the first cloud adapter is a drop-in.

**Amended in place, no ADR-0007.** Nothing has shipped and ticket 21 removed the compatibility promise, so there is no consumer to migrate; a new ADR would restate decisions that already have homes. The null-cost trap is called out in ADR-0001's Consequences, where a future contributor is most likely to "fix" it.

New glossary terms: Declared Rate, Cost Ceiling. Provider State and Provider Health entries corrected (`NOT_READY` never existed; Provider State now points at Provider Health), `recoverable` defined under SpeechEvent, RouterContext and RoutingDecision updated, Capabilities Honesty extended to cover declared rates.