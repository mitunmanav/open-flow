# 0004: Adaptive Dictation Router V1

Date: 2026-10-02

## Status

Accepted — amended 2026-10-02 (`.scratch/openflow-v1/issues/22-providerhealth-cost-shape.md`): rules given a fixed evaluation order and renumbered; rule 4 rewritten against the real `ProviderHealth` set; cost ceiling now compared against a worst-case bound; empty candidate sets now attributed to a rule.

Amended 2026-10-03 (`.scratch/openflow-v1/issues/31-latency-guard.md`): rule 5 gains a second input — the Latency Guard, which narrows a verdict from measured Final Latency between dictations rather than from provider-reported truth. Rules renumbered 6 and 7.

## Context

Provider neutrality is worthless if selection logic is scattered across the controller, settings, and bubble. Centralizing it lets the user control privacy/cost, and gives one place to log what actually happened for transparency.

## Decision

`AdaptiveDictationRouter.choose(providers, request, context): RoutingDecision` is the single selection point; the controller calls it at `PREPARING`. The router returns a decision, it does not own the event stream.

`RouterContext` carries: `privacyMode` (local-only / cloud-allowed), `offline` (no network), `costCeilingMicrosUsd: Long?` (`null` = no ceiling — an absent ceiling is not a zero ceiling; per-dictation, explicitly not a monthly budget), `preferredProviderId` (manual preference).

Eligibility is evaluated in a fixed order, and the order is part of the contract because "which rule excluded this provider" is the user's explanation:

1. Manual preference — a ranking override only, never above a hard exclusion.
2. `privacyMode = local-only` → cloud entries excluded. `offline = true` → online entries excluded. Consent first: these are the user's explicit instruction, not the router's judgement.
3. Capability match for the request (language support, offline-if-required).
4. Cost ceiling: the **worst case** — `pricing.microsUsdPerSecond × maxAudioDurationSeconds` — against `costCeilingMicrosUsd`. The router runs at `PREPARING`, before a sample exists, so it cannot compare a realised amount; a ceiling that can be exceeded after the router has committed is not a ceiling. Known-free (`0`) passes. `pricing = null` (cannot estimate) is **not** blocked by a ceiling, but ranks below every known-cheap candidate — treated as infinity it would make a legitimately free provider unusable, treated as zero it would quietly overspend.
5. `health()`: `UNAVAILABLE` and `MODEL_MISSING` skip; `DEGRADED` is eligible but ranked last. Health is normally the provider's own cheap local truth, never an active network ping, but rule 6 may narrow it. Health is last among exclusions because it is the most volatile and least authoritative signal. Health never empties the candidate set: if skipping would leave nothing, `DEGRADED` entries are admitted and the decision is recorded as `DEGRADED_LAST_RESORT`. With one real provider and `privacyMode = local-only`, excluding a mildly degraded engine would otherwise leave the user unable to dictate at all.
6. **The Latency Guard — rule 5's second input.** After each dictation the router folds the Final Latency it just observed into a per-provider rolling window, smooths it, and while the smoothed value sits above the threshold it narrows that provider's verdict to `DEGRADED` for the next `choose()`. Narrowing only; the guard can never upgrade a verdict.
7. Nothing else. The order ends here, and a provider that survives every rule is chosen.

**Why rule 6 acts only between dictations.** The verdict is written when a dictation ends and read by `choose()` at the next `PREPARING` — the same snapshot this ADR already reads, written one dictation late. Nothing acts within a live utterance: no mid-dictation model swap, no mid-dictation thread change. That is what keeps rule 6 on the right side of the line against automatic health-based rerouting, and it is also why rule 6 needs no new event and no new eligibility rule.

**Why rule 6 smooths rather than thresholding raw.** Thermal throttling is the normal case on a phone, not the edge case: the same device legitimately decodes slower at minute three than at minute one. A hard threshold trips late in every long dictation and never clears — a control that looks configured and is noise. The question is not *is this device fast* but *is this device fast right now, for this person*. Hysteresis on both edges, because a value hovering at the line must not flap the verdict between dictations.

**The threshold is provisional and says so.** V1 ships `docs/providers/model-selection.md`'s stated budget — Final within ~1 s of endpoint — because it is the project's own number, already a duration, already in the units measured here. No runtime threshold can be calibrated until measurements exist on real hardware; a provisional number that is honestly labelled beats both an invented constant and shipping no guard at all, which would leave the unmeasured model default exactly as un-falsifiable as it is today. Recalibrate against the benchmark's numbers when they land.

**Rule 6 offers no remedy, and the app says so once.** Every bundled English streaming model larger than the en-20M default is slower, and the offline-tier models are both larger and a different mode, so a device that is too slow has nothing faster to fall back to — V1 ships one real provider. The app states the limitation once per session, the limit set in mono beside what still works, then stays quiet: a warning firing on every dictation trains dismissal, and on a genuinely slow device it would be permanent. Final Latency stays visible per dictation in History, so the truth remains one tap away rather than only ever being announced once.

`preferredProviderId` is first choice among eligible entries — but on hard failure (model missing, can't load) the router falls back to the next eligible local provider and records that fallback; it never silently sends audio to a provider class the user didn't allow. Fallback is only to entries whose capabilities satisfy the same request. Fallback trigger: `FailureReason != Cancelled && recoverable`.

**Empty candidate sets are still decisions.** `RoutingDecision` returns `chosen = null` alongside `blockedBy` (the first rule that emptied the set) and `rejected` (every provider with its excluding rule). Without this the bubble can only say "no provider available", when the truth may be "no speech model installed", "nothing allowed offline", or "over your cost ceiling" — three failures with three completely different fixes.

Logging: every dictation records chosen provider, candidates considered, deciding rule, fallback chain, final latency, `estimatedCostMicrosUsd`, `worstCaseMicrosUsd`, `ceilingAppliedMicrosUsd`, and final outcome — including when rule 6 narrowed the verdict, and why. Local rolling window of last-N final latency/reliability stored per provider for V1 visibility, which is the same window rule 6 reads; automatic health-based rerouting is a later decision.

UI: history shows provider + latency + network path + estimated cost per dictation, rendering `unknown` in mono when `pricing = null` — never `$0.00` and never blank, since a blank reads as free to exactly the privacy-conscious user this app is for. About shows the active rule summary.

No user-facing cost control ships in V1: with no billable provider, a ceiling slider would control nothing. The field exists so the first cloud adapter is a drop-in.

## Consequences

Manual preference + privacy/offline/cost rules + fallback are shipped; automatic ML routing is explicitly out of V1. When it arrives it reads the same decision log — the measurement groundwork is laid.
