# Documentation

Every document this project publishes is listed here, and the project site is
described at the end. `docs-check` fails the
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
| [0007-build-toolchain-and-sdk-levels.md](adr/0007-build-toolchain-and-sdk-levels.md) | Build toolchain, SDK levels, `minSdk` 26 |
| [0008-acceptance-gate-two-bars.md](adr/0008-acceptance-gate-two-bars.md) | The acceptance gate's two bars, and tag-driven enforcement |
| [0009-automated-dependency-landing-path.md](adr/0009-automated-dependency-landing-path.md) | How an automated dependency bump reaches `main` |

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
| [provider-authoring.md](providers/provider-authoring.md) | Implementing `SpeechProvider` and registering the adapter |
| [provider-testing.md](providers/provider-testing.md) | Capabilities Honesty and the shared Contract Test suite |
| [provider-proposal.md](providers/provider-proposal.md) | When to propose a provider, and the licensing obligations |

The proposal criteria themselves live in
[`.github/ISSUE_TEMPLATE/provider_proposal.md`](../.github/ISSUE_TEMPLATE/provider_proposal.md),
which is their source of truth; the proposal guide links to it rather than
restating it, so the two cannot drift apart.

## Privacy

| Document | What's in it |
| --- | --- |
| [privacy-policy.md](privacy/privacy-policy.md) | What we collect, which is the shortest answer being nothing |
| [permissions-policy.md](privacy/permissions-policy.md) | Every Android permission, why it is needed, and the Play disclosure text |

## Quality

| Document | What's in it |
| --- | --- |
| [acceptance-gate.md](quality/acceptance-gate.md) | The acceptance gate: two bars, fourteen scenarios, and how a run is recorded |

That document carries a fenced `json` status block that CI parses and **no prose
outside it is ever parsed**. It is embedded in the human record on purpose, so coverage
and its prose cannot drift apart and a reworded table cannot change the gate.

## Contributing and agent guides

| Document | What's in it |
| --- | --- |
| [issue-tracker.md](agents/issue-tracker.md) | Where issues live as files, and how they are named |
| [triage-labels.md](agents/triage-labels.md) | The five triage roles, in this repository's exact strings |
| [domain.md](agents/domain.md) | How to read the glossary and the ADRs before working |

## The project site

[`website/`](../website/) holds four hand-written pages — Home, How it works,
Privacy, Get involved — published by `.github/workflows/pages.yml` to
`mitunmanav.github.io/open-flow`. There is no generator and no build step;
GitHub Pages serves the directory as committed, which is why every internal
link in it is relative.

The site is not a copy of these documents and does not render them. It links to
them, names the one each section came from, and uses the four as its source
material. Documentation lives here; the site is what points at it.

The site's typefaces are committed as variable `woff2` under
`website/fonts/` rather than loaded from a font CDN, so that loading the page
contacts no third party. `THIRD_PARTY_NOTICES.md` records their licences.

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
- Every page under `website/` lists every page, in the same order, in its nav and
  in its footer. Four hand-written pages with two copies of that list will drift
  the moment someone adds a page, and nothing else would notice.
- Every relative `href`, `src` and CSS `url()` under `website/` resolves on
  disk. With no build step there is no compiler to complain about a renamed
  stylesheet; the symptom is a silently unstyled page.