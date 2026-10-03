# Proposing a provider

When to propose a speech-to-text engine, and what a proposal has to settle before
review can start.

**The criteria are not restated here.** They live in the issue template, which is
the source of truth:

> [`.github/ISSUE_TEMPLATE/provider_proposal.md`](../../.github/ISSUE_TEMPLATE/provider_proposal.md)

If this guide and that template ever disagree, the template wins — so fix the
template, not this page. Everything below is context for filling it in.

Read [`GLOSSARY.md`](../../GLOSSARY.md) first. `Provider Adapter`, `Capabilities
Honesty`, `Declared Rate`, and `Provider Health` all mean something specific
here.

## How a provider actually lands

There is no plugin system, no runtime discovery, and no external write access. A
provider arrives as a **pull request** adding `providers/<name>/` plus one
registry entry in `app/`, subject to CI and review. That is the whole mechanism,
and it is the same mechanism our own two adapters went through.

This is deliberate
([ADR-0006](../adr/0006-provider-authoring-no-sdk.md)): a third-party adapter
that could be dropped in at runtime would be a public extension API, and V1 has
not earned one.

## When to propose

Propose when all of these hold:

- **You can state that the model weights may be redistributed.** This is the
  gate, and it is checked first — see below.
- **It fits the existing contract.** If it needs a new method on `SpeechProvider`,
  a new capability, or a change to
  [ADR-0001](../adr/0001-speech-provider-contract.md), say so in the proposal
  rather than in the patch. Each of those affects every existing adapter, so a
  contract change is a design conversation, not an implementation detail.
- **You can declare capabilities honestly**, with evidence. See
  [provider-testing.md](provider-testing.md) for what "evidence" means — it is a
  passing run of the shared Contract Test suite, not a claim.
- **You will accept the review.** Passing CI, including the documentation and
  attribution gates, which are required on every pull request.

Do not open a proposal to ask whether an engine is *worth adding in principle*.
Open it with the answers filled in. An engine with no redistribution answer, or
with capabilities asserted rather than demonstrated, is not reviewable.

## Licensing: the model licence is the gate

**The licence that disqualifies a provider is the model licence, not the code
licence.** Engines are usually permissively licensed — MIT, Apache-2.0, and so
on — while their weights frequently are not, and redistribution is precisely
what an Android APK does. A permissive engine with non-redistributable weights
cannot ship, no matter how good it is, and this is the single most common way a
proposal stalls.

So state, separately:

- the licence of the **engine**, and
- the licence of the **model weights**, and
- whether those weights may be **redistributed** inside a distributed APK.

If you cannot state that redistribution is permitted, the proposal is blocked
until you can. It does not matter whether the model is downloadable by the user
instead — OpenFlow's local-first design means weights are fetched and used
on-device, and the licence terms of that distribution are the thing in question.
Read them; do not infer permissiveness from the engine's licence.

OpenFlow itself is Apache-2.0, and that does not extend to anything you add.

## What a provider author must add

### `THIRD_PARTY_NOTICES.md`

Add a section to the root
[`THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md) covering the engine and
its dependencies: what it is, where it came from, its licence, and its
copyright notice as the licence requires. The file is the project's record of
what ships inside the binary, so a provider that adds a dependency without a
notice leaves it incomplete — which is the same defect as a stale document.

### Model licence text

Any model licence requiring attribution or redistribution has its full text
dropped in `providers/<name>/licenses/`, next to the adapter rather than in the
repository root. Point at that directory from your `THIRD_PARTY_NOTICES.md`
section, and say which weights it covers — a directory of licence files with no
mapping to specific models tells a reader nothing.

### Attribution

No bot or AI assistant may appear as an author or co-author, and this is enforced
by a required CI check rather than a convention. Human co-authors are welcome,
including the trailers GitHub appends when it squashes a pull request. The rule
and its reasoning are in [`AGENTS.md`](../../AGENTS.md).

## What the proposal must answer

Fill in the template. In outline, the sections exist to establish:

- **Engine and licence** — the table above, and the redistribution answer.
- **Capabilities to declare** — each capability marked declared or not, with the
  evidence that it works. `0` means free and should be stated as such;
  `unknown` is also acceptable and is not the same as free.
- **Integration shape** — whether the contract needs changing. Answer this
  honestly; a contract change found during review is a rejected patch, and
  finding it in the proposal costs nobody anything.
- **Health reporting** — for each of `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, and
  `MODEL_MISSING` your adapter can return: what condition produces it, and what
  the user is expected to do about it. `MODEL_MISSING` means a download prompt;
  `UNAVAILABLE` means a retry. These are three different remediations, which is
  why they are three different values.
- **Tests** — how you will run the shared Contract Test suite locally, and the
  output of a passing run.
- **Alternatives** — the other engines you considered, and why this one.

## The two adapters already here

Both are ours, and both went through this process:

- **`FakeProvider`** — scripted events, injected latency, and injected failures.
  Runs the whole pipeline with no engine, and is how the contract is tested
  without a model on disk.
- **`SherpaOnnxProvider`** — the first real provider. Research, including the
  streaming/offline/VAD/endpointing analysis and the model matrix, is in
  [sherpa-onnx.md](sherpa-onnx.md) and [model-selection.md](model-selection.md).

Read `sherpa-onnx.md` before proposing anything that also runs locally: the
questions about partial transcripts, endpointing, and CPU cost have already been
answered for that family, and a proposal that reopens them should say why.

## Then what

1. Open the issue from the template.
2. Discussion happens there, not in the pull request.
3. When the shape is agreed, open the pull request adding `providers/<name>/`
   and the registry entry, with the licence obligations above discharged.

[provider-authoring.md](provider-authoring.md) is the contract and the registry
entry; [provider-testing.md](provider-testing.md) is the suite your adapter has
to pass.