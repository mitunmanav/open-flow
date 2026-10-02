# 0004: Adaptive Dictation Router V1

Date: 2026-10-02

## Status

Accepted — amended 2026-10-02 (`.scratch/openflow-v1/issues/22-providerhealth-cost-shape.md`): rules given a fixed evaluation order and renumbered; rule 4 rewritten against the real `ProviderHealth` set; cost ceiling now compared against a worst-case bound; empty candidate sets now attributed to a rule.

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
5. `health()`: `UNAVAILABLE` and `MODEL_MISSING` skip; `DEGRADED` is eligible but ranked last. Health is provider-reported cheap local truth, never an active network ping. Health is last among exclusions because it is the most volatile and least authoritative signal. Health never empties the candidate set: if skipping would leave nothing, `DEGRADED` entries are admitted and the decision is recorded as `DEGRADED_LAST_RESORT`. With one real provider and `privacyMode = local-only`, excluding a mildly degraded engine would otherwise leave the user unable to dictate at all.

`preferredProviderId` is first choice among eligible entries — but on hard failure (model missing, can't load) the router falls back to the next eligible local provider and records that fallback; it never silently sends audio to a provider class the user didn't allow. Fallback is only to entries whose capabilities satisfy the same request. Fallback trigger: `FailureReason != Cancelled && recoverable`.

**Empty candidate sets are still decisions.** `RoutingDecision` returns `chosen = null` alongside `blockedBy` (the first rule that emptied the set) and `rejected` (every provider with its excluding rule). Without this the bubble can only say "no provider available", when the truth may be "no speech model installed", "nothing allowed offline", or "over your cost ceiling" — three failures with three completely different fixes.

Logging: every dictation records chosen provider, candidates considered, deciding rule, fallback chain, per-attempt latency, `estimatedCostMicrosUsd`, `worstCaseMicrosUsd`, `ceilingAppliedMicrosUsd`, and final outcome. Local rolling window of last-N latency/reliability stored per provider for V1 visibility; automatic health-based rerouting is a later decision.

UI: history shows provider + latency + network path + estimated cost per dictation, rendering `unknown` in mono when `pricing = null` — never `$0.00` and never blank, since a blank reads as free to exactly the privacy-conscious user this app is for. About shows the active rule summary.

No user-facing cost control ships in V1: with no billable provider, a ceiling slider would control nothing. The field exists so the first cloud adapter is a drop-in.

## Consequences

Manual preference + privacy/offline/cost rules + fallback are shipped; automatic ML routing is explicitly out of V1. When it arrives it reads the same decision log — the measurement groundwork is laid.
