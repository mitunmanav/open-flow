# Provider SDK documentation

Type: grilling
Status: resolved
Blocked by: none

## Question

What does a third-party adapter author need to ship a provider? A `docs/providers/` guide: implementing `SpeechProvider` (ADR-0001), capabilities honesty rules, health/estimated-cost reporting, contract-test expectations, registry entry, model licensing/THIRD_PARTY_NOTICES obligations, and a provider proposal template (from the automation ticket). Is any part of this a public SDK artifact (a separate `provider-api` module) in V1, or docs-only?

## Answer

Provider-authoring docs are **docs-only and public-facing** in V1. No SDK artifact.

**Framing.** V1 has exactly two `SpeechProvider` implementations — `FakeProvider` and `SherpaOnnxProvider` — and both are ours. There is no third-party adapter author in V1, so the guide's audience is hypothetical and the real question is the posture we take toward a future contributor.

**Doc set** — three files in `docs/providers/`, split by concern because they have different audiences and change rates (the contract moves when ADR-0001 is revised; licensing obligations move separately):

- `provider-authoring.md` — the contract + registry entry
- `provider-testing.md` — capabilities honesty + contract tests
- `provider-proposal.md` — licensing/`THIRD_PARTY_NOTICES` obligations + when to propose

**No `provider-api` module in V1.** Contract types stay in `core/stt` per ADR-0005, honouring the standing "no empty modules" preference — standing up a module before a second real provider exists is exactly what that rule forbids. Recorded in `docs/adr/0006-provider-authoring-no-sdk.md`.

**No compatibility promise.** `provider-authoring.md` opens with an explicit statement that `SpeechProvider` is an internal contract, not a stable API: it may change within the 1.x line, and ADR-0001 revisions plus the CHANGELOG announce changes. There is no artifact to depend on, so a promise would be honoured by refusing to fix our own bugs — and because the guide is public-facing, the statement is what stops an outsider filing a bug against "you changed your API."

**Capabilities honesty is enforced by tests, not runtime probes.** The shared suite asserts behaviour against declaration: declare `partialTranscripts = true` → must emit `Partial`; a declared language → `transcribe` for it must not fail `UnsupportedLanguage`; `streaming = false` → the controller must not depend on streaming. ADR-0001 lets capabilities drive *all* app behaviour with nothing verifying them, so a provider that under-delivers silently degrades UX rather than failing. Runtime validation at `prepare()` is an explicit V1 non-goal: it duplicates test coverage and would catch the mismatch mid-dictation, the worst possible moment.

**The suite lives in `core`'s test fixtures** (Gradle `testFixtures` / test-only variant), consumed by provider modules. Keeps the module count at three and makes "passed the shared suite" a compile-time fact rather than a review opinion. No `provider-testkit` module.

**Health and cost reporting are documented only as far as ADR-0001/0004 define them.** Both are under-specified today — see ticket 22 for the gap and its resolution.

**Licensing obligations.** A provider author adds a section to root `THIRD_PARTY_NOTICES.md`; any model license requiring attribution or redistribution drops its text in `providers/<name>/licenses/`. One file a release engineer can audit beats an aggregation step that can silently miss a provider. **Hard rule: a provider cannot ship if its model license forbids redistribution** — sherpa-onnx's models carry their own terms and this is the trap most likely to bite.

**Proposal criteria are cross-linked, not restated.** The provider-proposal GitHub issue template (settled in ticket 15) is the single source of truth; the guide keeps only the reasoning. Forward reference: no `.github/ISSUE_TEMPLATE/` exists yet, so the link points at a file still to be built (ticket 23).

**Third-party providers land as PRs.** V1 has no plugin system and no external write access. A provider arrives as `providers/<name>/` plus one registry entry in `app/` (ADR-0005, `docs/architecture/modules.md`), subject to CI and review.

Glossary terms added: Provider Adapter, Provider Health, Capabilities Honesty, Contract Test.
