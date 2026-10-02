# Write the three provider guides

Type: task
Status: open
Blocked by: none

## Question

Ticket 21 recorded the provider-authoring **decision** and marked itself resolved, but its three promised guides were never written. `docs/providers/` contains only `sherpa-onnx.md` and `model-selection.md`. ADR-0006 and `docs/README.md` both promise `provider-authoring.md`, `provider-testing.md`, and `provider-proposal.md`, so this is a false record on the canonical map, not a missing nice-to-have.

Write all three in `docs/providers/`, split by concern and audience as ticket 21 decided:

- **`provider-authoring.md`** — the `SpeechProvider` contract (ADR-0001) and the registry entry in `app/`. Must open by stating that `SpeechProvider` is an internal contract, not a stable API: it may change within 1.x, and ADR revisions plus the CHANGELOG announce changes. That sentence is load-bearing; without it a contributor will write against the contract as if it were stable.
- **`provider-testing.md`** — Capabilities Honesty and the Contract Test suite in `core`'s test fixtures. A declared capability is binding: declare `partialTranscripts` and you must emit `Partial`; declare a language and `transcribe` must not fail `UnsupportedLanguage`. Cover `ProviderHealth` (four values, cached local truth, no probe) and `pricing` (`0` is free, `null` is unknown, and null is **not** zero — that trap is ADR-0001's stated consequence).
- **`provider-proposal.md`** — licensing and `THIRD_PARTY_NOTICES` obligations, and when to propose. Cross-link `.github/ISSUE_TEMPLATE/provider_proposal.md` as the source of truth for criteria rather than restating them; that template now exists and is authoritative.

These four references are the current contents of `.github/docs-baseline.txt`, and they are the only four. When the guides land, `docs-check` fails until each baseline line is deleted in the same commit — that failure is the reminder, so do not suppress it.

**Watch the `ProviderHealth` / `ProviderState` split** from ticket 22: four health values, kept separate from lifecycle, and no health value mirrors a lifecycle value. `MODEL_MISSING` means a download prompt, `UNAVAILABLE` means a retry.

Suggested order: `provider-authoring.md`, then `provider-testing.md`, then `provider-proposal.md`. All three in one commit — a half-written guide set fails the same gate a missing one does.