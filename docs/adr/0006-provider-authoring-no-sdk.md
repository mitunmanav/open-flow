# 0006: Provider authoring is docs-only in V1 — no SDK artifact

Date: 2026-10-02

## Status

Accepted

## Context

The provider seam is V1's headline architectural bet: ADR-0001 fixes a stable `SpeechProvider` contract and ADR-0005 keeps providers on the far side of a hard dependency rule, so the router, refiner, and insertion layers never learn which engine answered. But V1 ships exactly two implementations — `FakeProvider` for tests and `SherpaOnnxProvider` for real dictation — and both are ours. There is no third-party adapter author in V1.

That gap invites two opposite mistakes. Ship a `provider-api` module before any second provider exists, turning an unused seam into an empty module and a public API-stability commitment we would then have to honour by refusing to fix our own bugs. Or keep the seam a private secret and leave a future contributor no template at all. The repo is Apache-2.0 and open source, so publishing provider-authoring documentation costs barely more than writing it for ourselves — the difference is tone and honesty, not effort.

## Decision

V1 ships provider-authoring **documentation only**, written to be genuinely public-facing:

- Three guides in `docs/providers/`: `provider-authoring.md` (contract + registry entry), `provider-testing.md` (capabilities honesty + contract tests), `provider-proposal.md` (licensing obligations + when to propose).
- **No `provider-api` module.** The contract types stay in `core/stt` per ADR-0005.
- **No compatibility promise.** `provider-authoring.md` states up front that `SpeechProvider` is an internal contract, not a stable API; it may change within the 1.x line, and ADR-0001 revisions plus the CHANGELOG announce changes.
- Capabilities honesty is enforced by the shared Contract Tests in `core`'s test fixtures, not by runtime validation at `prepare()`.
- Third-party providers land as pull requests. V1 has no plugin system and no external write access.

## Considered Options

- **Docs written for ourselves only.** Cheapest, but an open-source repo that hides its extension seam wastes the main thing that makes the project attractive to contributors.
- **A separate `provider-api` module.** Would give adapters a compilable surface, but before a second real provider exists it is an empty module — the exact outcome ADR-0005's "no empty modules" rule forbids — and it converts a documentation decision into an API-stability commitment plus a publishing pipeline, with no demand signal to justify either.
- **A published SDK (Maven artifact or separate repo, versioned independently).** Real ongoing maintenance: semantic versioning of the contract, a compatibility policy, and a release cadence. Nothing in V1 needs it.

## Consequences

A future contributor has a template but no promise, and the "not a stable API" line is load-bearing — without it, an outsider will reasonably assume `SpeechProvider` is stable and write against it. Adding `provider-api` later is still open; it becomes worth doing when a second real provider exists, at which point it is a breaking restructure of where the contract types live, not a new decision.

Provider-neutrality in V1 is therefore partly speculative: the contract is real and tested, but only ever exercised by two implementations we wrote. The honest test of whether the seam works arrives with the second provider, not with the first.