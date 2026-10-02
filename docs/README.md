# Documentation

Every document this project publishes is listed here. `docs-check` fails the
build if a document under `docs/` is not reachable from this page, so an index
entry cannot go stale the way a link in prose does.

Start with [`GLOSSARY.md`](../GLOSSARY.md) for vocabulary and
[the ADRs](adr/) for decisions.

## Architecture decisions

Decisions are numbered from `0001` and never renumbered. An ADR is amended in
place when the reasoning changes, with the amendment noted in its `## Status`
section, because a decision that has been reversed silently is a decision
nobody can trust.

| ADR | Decision |
| --- | --- |
| [0001-speech-provider-contract.md](adr/0001-speech-provider-contract.md) | SpeechProvider contract shape |
| [0002-dictation-state-machine.md](adr/0002-dictation-state-machine.md) | Dictation state machine |
| [0003-safe-text-insertion.md](adr/0003-safe-text-insertion.md) | Safe text insertion |
| [0004-adaptive-dictation-router.md](adr/0004-adaptive-dictation-router.md) | Adaptive Dictation Router V1 |
| [0005-module-layout.md](adr/0005-module-layout.md) | Expandable module layout |
| [0006-provider-authoring-no-sdk.md](adr/0006-provider-authoring-no-sdk.md) | Provider authoring is docs-only in V1 |

## Architecture

| Document | What's in it |
| --- | --- |
| [modules.md](architecture/modules.md) | The three modules, their dependency rule, and the seams |
| [refiner.md](architecture/refiner.md) | The eight deterministic TranscriptRefiner stages |
| [reference-apps.md](architecture/reference-apps.md) | Teardown of the existing bubble dictation apps, and what we may reuse |

## Providers

| Document | What's in it |
| --- | --- |
| [sherpa-onnx.md](providers/sherpa-onnx.md) | sherpa-onnx streaming, offline, VAD, and endpointing on Android |
| [model-selection.md](providers/model-selection.md) | The benchmark matrix and the V1 default model |

Not yet written, and promised by ADR-0006: `provider-authoring.md`,
`provider-testing.md`, `provider-proposal.md`. The proposal template exists
today at [`.github/ISSUE_TEMPLATE/provider_proposal.md`](../.github/ISSUE_TEMPLATE/provider_proposal.md)
and is the source of truth for proposal criteria until the guide lands.

## Privacy

| Document | What's in it |
| --- | --- |
| [privacy-policy.md](privacy/privacy-policy.md) | What we collect, which is the shortest answer being nothing |
| [permissions-policy.md](privacy/permissions-policy.md) | Every Android permission, why it is needed, and the Play disclosure text |

## Contributing and agent guides

| Document | What's in it |
| --- | --- |
| [issue-tracker.md](agents/issue-tracker.md) | Where issues live as files, and how they are named |
| [triage-labels.md](agents/triage-labels.md) | The five triage roles, in this repository's exact strings |
| [domain.md](agents/domain.md) | How to read the glossary and the ADRs before working |

## Writing rules

`docs-check` enforces these mechanically; they are written down so a new
contributor does not have to read the script to know the house style.

- A document may not link to a file that does not exist.
- A document may not name a backticked file path that does not exist, unless the
  sentence marks it as forward-looking ("once", "planned", "if it exists") or the
  directory it would live in does not exist yet.
- Backticked `CamelCase.kt`-style references are read as types in a third-party
  library, not files we owe. Our own files are `kebab-case`.
- ADR filenames are contiguous from `0001`, and each ADR's title, date, and
  status must agree with its filename.
- Every document here is reachable from this page.