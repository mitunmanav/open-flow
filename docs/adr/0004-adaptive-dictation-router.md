# 0004: Adaptive Dictation Router V1

Date: 2026-10-02

## Status

Accepted

## Context

Provider neutrality is worthless if selection logic is scattered across the controller, settings, and bubble. Centralizing it lets the user control privacy/cost, and gives one place to log what actually happened for transparency.

## Decision

`AdaptiveDictationRouter.choose(providers, request, context): RoutingDecision` is the single selection point; the controller calls it at `PREPARING`. The router returns a decision, it does not own the event stream.

`RouterContext` carries: `privacyMode` (local-only / cloud-allowed), `offline` (no network), `costCeiling`, `preferredProviderId` (manual preference).

Rules:

1. If `preferredProviderId` is set, it is first choice — but on hard failure (model missing, can't load) the router falls back to the next eligible local provider and records that fallback; it never silently sends audio to a provider class the user didn't allow.
2. `privacyMode = local-only` → cloud entries excluded. `offline = true` → online entries excluded.
3. `costCeiling` blocks any entry whose estimated per-request cost exceeds it (cloud adapters must report cost; local entries report 0).
4. `health()` filters: `NOT_READY`/`UNAVAILABLE` skip; `DEGRADED` sinks. No active network ping in V1 — health is provider-reported cheap local truth.
5. Fallback is only to entries whose capabilities satisfy the same request (language support, offline-if-required). Fallback trigger: `FailureReason != Cancelled && recoverable`. Never falls back from local to cloud unless the original attempt was cloud-allowed.

Logging: every dictation records chosen provider, candidates considered, deciding rule, fallback chain, per-attempt latency, final outcome. Local rolling window of last-N latency/reliability stored per provider for V1 visibility; automatic health-based rerouting is a later decision.

UI: history shows provider + latency + network path + estimated cost per dictation; About shows the active rule summary.

## Consequences

Manual preference + privacy/offline/cost rules + fallback are shipped; automatic ML routing is explicitly out of V1. When it arrives it reads the same decision log — the measurement groundwork is laid.
