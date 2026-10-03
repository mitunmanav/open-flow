# Write the three provider guides

Type: task
Status: resolved
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

## Answer

All three guides are written, in the suggested order, and `docs-check` is green with the baseline deleted.

- **`docs/providers/provider-authoring.md`** — opens by stating that `SpeechProvider` is an internal contract, not a stable API, and that ADR revisions plus the CHANGELOG announce changes. Then the four methods plus the state stream, `SpeechEvent`, the `FailureReason` enum with `recoverable` defined as *the dictation can still complete by another route*, the ten capabilities as a table, `health()` as cached local truth, one-instance-per-model-configuration, and the registry entry in `app/`.
- **`docs/providers/provider-testing.md`** — Capabilities Honesty as the reason a shared suite exists at all, then what it asserts: a declared capability is binding, `supportedLanguages` checked across the whole declared set, `Partial` snapshots are cumulative, the flow is cold and cancellation stops the engine, the `READY` → health ∈ {`HEALTHY`, `DEGRADED`, `UNAVAILABLE`} invariant and its converse (`MODEL_MISSING` pairs with `OfflineModelMissing`), `health()` answers with no network, and the `0` vs `null` pricing trap with its cost consequence.
- **`docs/providers/provider-proposal.md`** — states up front that the criteria live in the issue template and that the template wins on conflict, then the licensing position: **the model licence is the gate, not the code licence**, because redistribution is what an APK does. Adds the `THIRD_PARTY_NOTICES.md` section, model licence text under `providers/<name>/licenses/`, and the attribution rule. Criteria are linked, never restated, per the ticket.

### The ProviderHealth / ProviderState split

Carried through all three without merging them. The glossary's own warning — *"`READY` and `HEALTHY` are not two values of one enum, and no health value mirrors a lifecycle value"* — is quoted as the reason the split exists, with `CLOSED` named as something the app did rather than something an engine reported. `MODEL_MISSING` is a download prompt; `UNAVAILABLE` is a retry; `DEGRADED` ranks last rather than sinking.

### The false record is closed

`docs/README.md` no longer says the guides are "not yet written" — it lists all three and points at the issue template for criteria. `CHANGELOG.md` records them under Added, which is the same channel ADR-0006 nominates for contract changes.

The four baseline lines were the file's only entries, so `.github/docs-baseline.txt` is **deleted**, as its own header instructs when the last entry goes. The ratchet fired first, exactly as this ticket predicted — all four entries reported as stale — and `python3 .github/scripts/check_docs.py` now exits 0 against 34 markdown files with no baseline at all.

### What was deliberately left as fog

Nothing graduated. The guides restate settled decisions (ADRs 0001, 0005, 0006), so no new decision surfaced. Their forward references to `core`'s test fixtures and the `app/` registry entry are honest about the code not existing yet and are marked forward-looking, since ticket 27 is what lands them.

Not pushed — left for the owner, as ticket 23 was.