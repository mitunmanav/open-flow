# Adaptive Dictation Router V1 spec

Type: grilling
Status: resolved
Blocked by: 04

## Question

Specify the V1 router: manual preferred provider, privacy/offline/cost rules, health checks, fallback order, and the routing decision log format (visible per-dictation: provider, latency, no-network path, insert success). Deliberately no auto-ML.

## Answer

Router V1 settled (detail in `docs/adr/0004-adaptive-dictation-router.md`):

- `choose(providers, request, RouterContext): RoutingDecision`; controller calls it at PREPARING; router doesn't own the event stream.
- Rules: manual preference first (hard-fail falls back, never silently to cloud), privacyMode/offline/costCeiling filters, health filters (NOT_READY/UNAVAILABLE skip, DEGRADED sinks, no active probe), capability-matched fallback on recoverable non-cancelled failures.
- Every dictation logs chosen provider, candidates, deciding rule, fallback chain, per-attempt latency, outcome. Local rolling window stored for visibility; auto-rerouting later.
- UI: history row shows provider/latency/network path/cost; About shows rule summary.
