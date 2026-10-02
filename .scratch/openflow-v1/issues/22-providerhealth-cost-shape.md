# Specify ProviderHealth and the cost-reporting shape

Type: grilling
Status: open
Blocked by: none

## Question

Two contract gaps surfaced while documenting the provider seam (ticket 21). Both are defects in ADR-0001/0004, not documentation problems, so they get their own ticket.

**Health has two colliding vocabularies and no declared type.** ADR-0001 defines `ProviderState` = `NOT_PREPARED/PREPARING/READY/CLOSED` and lists `health()` as a method but never gives it a return type. ADR-0004 rule 4 then filters on `NOT_READY`/`UNAVAILABLE`/`DEGRADED` — none of which are `ProviderState` values — so `READY` (lifecycle) and `NOT_READY` (health) read like two values of one enum when they are different concepts. Neither set can express what `docs/providers/sherpa-onnx.md` already requires: a **"model missing"** health state, which `FailureReason.OfflineModelMissing` covers during a call but not in the pre-call filter the router runs. Decide: the `ProviderHealth` type and its state set (including a `HEALTHY` counterpart and a model-missing case); whether health and `ProviderState` stay separate types or merge; the `health()` return type under ADR-0004's "cheap local truth, no active probe" constraint; and the `MODEL_MISSING` vs `FailureReason.OfflineModelMissing` boundary.

**Cost reporting is a flag where an amount is needed.** `estimatedCostAvailability` is a boolean capability, but ADR-0004 rule 3 compares an estimated *per-request cost* against `costCeiling`. A boolean cannot express an amount. Decide the cost-reporting shape — per-request estimate, currency, and how an unknown/unavailable estimate is handled against a ceiling — and how it relates to the boolean capability.

Then revise ADR-0001 and ADR-0004 to match, and check the router's rule-4 filter and `ProviderState` in `GLOSSARY.md` against whatever is decided.