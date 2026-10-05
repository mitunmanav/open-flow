# How does publication select reviewed gate evidence and the tested APK?

Type: grilling
Status: resolved
Blocked by: 58

## Question

Which reviewed evidence revision and official signed candidate artifact does the
publishing workflow select, and how does it bind those inputs to the release tag
without rebuilding away the tested APK's identity?

[What granularity does a gate record have?](58-what-granularity-does-a-gate-record-have.md)
settles one active APK in the live status JSON, shared build identity, no cross-build
coverage, testing official signed candidates before publication, and publication of
the exact tested bytes. Its `commit` identifies APK source, not the revision containing
the evidence. Historical sections are excluded from release validation.

Test results arrive in PRs after an APK is built. Committing those results creates a
new repository revision; the selected source tag's embedded document cannot acquire
that later evidence. Reading the current branch implicitly would leave evidence selection
moving underneath a release run. Exact source checkout, deterministic tag/version
identity and immutable published APKs remain required by
[The release version must come from the tag, and it does not](50-release-version-from-tag.md).

Settle an executable workflow plan for:

- Producing and making an official signed candidate available before publication,
  tied to the intended release tag, source commit, version and SHA-256.
- Selecting and pinning a reviewed evidence revision after results have landed,
  including which revision supplies the protocol/required IDs and what selection
  is allowed on tag pushes, manual dispatch and reruns.
- Locating and retaining the tested APK for promotion, and verifying signature,
  metadata and checksum without substituting a different rebuild.
- Handling absent/mismatched evidence, unavailable candidate bytes, old-tag rebuilds
  and an intentional waiver while keeping published identity immutable and the
  reported coverage truthful.

Keep one canonical document and one live JSON block; release validation reads that
block only. Do not pool historical records or loosen the two release bars. Decide
the input selection and lifecycle here; implementation belongs to
[Make the release workflow enforce the acceptance gate](36-release-workflow-enforces-the-gate.md).
Consult GLOSSARY.md, grilling and domain-modeling.

## Comments

### 2026-10-05 — First decision round

Claimed as the next open, unblocked, unclaimed child. Consulted grilling,
domain-modeling, GLOSSARY.md and the resolved artifact-granularity/version policies.
Current release workflow does not implement exact tested-byte promotion. Evidence
committed after a candidate build must be selected independently of its source tag.

First round for the owner:

1. Should tag pushes build official signed candidates only, with publication a
   separate manual promotion operation? Recommend yes: promotion consumes retained
   candidate bytes and pinned evidence, rather than rebuilding. Explicit separation
   permits testing before publication and prevents a tag push from implicitly
   selecting changing evidence.
2. Should promotion require a full evidence commit SHA from protected main, using
   that same revision's live Gate Status and protocol/required IDs? Recommend yes:
   accept a reviewed main-history revision explicitly, not a moving branch ref or
   the APK source revision by default; preserve the selected SHA on reruns.

Candidate retention/location and tester access, protocol changes after testing,
input binding, and waiver/rerun failure behavior remain subsequent decisions.
No workflow implementation or ticket resolution has been made.

Fact-finding: current workflow rebuilds/publishes immediately and retains Actions APK
artifacts for 30 days. v4 artifact IDs identify immutable uploads; name-based selection
is insufficient because replacement gets a new ID. Downloads require GitHub sign-in
and repository read access and must precede expiry. Draft release assets are not a
public external-tester distribution route. These constraints will inform the next round.
Sources: [upload-artifact](https://github.com/actions/upload-artifact),
[artifact downloads](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts?tool=webui),
[dispatch API](https://docs.github.com/en/rest/actions/workflows#create-a-workflow-dispatch-event),
[reruns](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/re-run-workflows-and-jobs).

### 2026-10-05 — First round accepted

The owner accepted both recommendations: tag pushes produce official signed candidates
only; manual publication promotes the retained tested bytes using an explicitly supplied
full evidence commit SHA from protected main. Gate Status and protocol/required IDs are
read from that same evidence revision. Candidate identity and evidence SHA remain fixed
on reruns; no moving-main or latest-artifact selection is permitted.

Remaining decisions include retention/tester access, candidate binding and protocol
changes after testing, and missing/mismatched inputs versus an explicit coverage waiver.

### 2026-10-05 — Second round accepted

The owner accepted GitHub Actions candidate artifacts retained for 90 days, selected
by run ID, artifact ID and APK SHA-256. Tester access requires GitHub sign-in.
Expiry blocks promotion; the workflow must not silently replace lost bytes with a
rebuild. The owner also accepted fresh active runs after substantive changes to
scenario requirements, pass criteria or Device Class rules. Wording-only corrections
may retain runs after explicit owner review. The selected evidence revision supplies
the applicable requirements.

Retention basis: [GitHub repository Actions settings](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository).

Remaining frontier: exact candidate/provenance checks and promotion inputs, an explicit
record of protocol compatibility review, and limits of the existing coverage waiver.

### 2026-10-05 — Final round accepted

The owner accepted explicit promotion inputs and candidate provenance/identity checks;
testing-protocol SHA plus owner-reviewed compatibility in Gate Status; and waivers
restricted to insufficient coverage. Missing/expired bytes, invalid records and identity
mismatches remain hard failures. This completes the shared understanding.

## Answer

Resolved through three live rounds on 2026-10-05. This is an executable workflow plan;
production implementation stays with
[Make the release workflow enforce the acceptance gate](36-release-workflow-enforces-the-gate.md).

### Candidate creation and availability

A canonical tag push builds an official signed candidate only. Resolve and record its
full source commit, check out that exact tag/source, pass the tag's deterministic release
version to the build, and verify signature, APK version and source identity. It does not
publish a GitHub Release or require completed device coverage merely to supply a test APK.

Upload the APK and a candidate manifest as an immutable Actions artifact retained for
**90 days**, selected by numeric workflow run ID and artifact ID, not by name or latest
run. The manifest records tag, full source SHA, version name/code, signed APK SHA-256,
signer certificate identity and originating run identity. Artifact ID comes from the
upload result/API, not from a guessed name; the API must associate it with that run.
Use the APK's own checksum, not the enclosing artifact ZIP's digest.

Supply testers the artifact download link, identity manifest, expiry and protocol revision.
GitHub sign-in/read access is required; no public candidate Release namespace or separate
hosting service is added. Official candidates amend the old released-APK-only rule.
[Artifact access/expiry](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts?tool=webui),
[immutable uploads and IDs](https://github.com/actions/upload-artifact),
[90-day public-repository retention](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository).

### Reviewed evidence and protocol

Collect results in PRs to protected main. Manual publication requires a **full evidence
commit SHA reachable in protected main's reviewed history**, independently of APK source.
The owner selects that SHA explicitly; do not resolve a moving main ref, latest artifact
or the source tag's evidence automatically. Fetch the canonical gate document at that
SHA; its live JSON and same-revision protocol/required IDs are the applicable rules.
Documentation consistency must be validated at that revision. Source ABI/version facts
come from the exact candidate source and actual APK, not a later build-file revision.

Gate Status advances to **version 5**, adding top-level `protocol_commit` (full SHA of the
protocol used for all active tests) and `protocol_reviewed` (Boolean owner attestation
that those tests apply to the selected evidence revision's protocol). These are shared
context, not per-run duplication. With no active candidate, protocol identity is null and
review is false; publication requires a valid protocol revision and true review, including
for a zero-coverage waiver. The protocol revision is reviewed repository history, not a
moving branch name. Initial zero-coverage records identify the intended test protocol.

The evidence PR explains compatibility with its selected protocol. Substantive changes
to scenario requirements, pass criteria or Device Class rules invalidate active runs:
clear them and rerun using the new protocol, then record its SHA. Wording-only differences
can retain runs after explicit owner review. Automation checks the revision/attestation
and structured contract; it does not pretend to judge semantic equivalence. The full
later evidence SHA is a publication input rather than a self-referential field in that
commit's document. Unreviewed compatibility is not waivable.

### Manual promotion and fixed inputs

Dispatch publication **from protected main** with explicit `release_tag`, `evidence_sha`,
`candidate_run_id`, `candidate_artifact_id`, and optional `gate_waiver_reason`. The
workflow's dispatch ref selects trusted publisher code; the evidence SHA is a separate
input. Require the main dispatch ref, resolve its workflow revision, and record it.
The full source SHA is derived from the selected tag, then matched against the manifest,
artifact-run provenance and Gate Status rather than separately supplied by the caller.
[Dispatch API](https://docs.github.com/en/rest/actions/workflows#create-a-workflow-dispatch-event).

Verify artifact/run association, same-repository official candidate workflow provenance,
successful candidate production, selected tag/source and deterministic version, expected
release signing certificate, and signed APK SHA-256 against both manifest and active
Gate Status. Read candidate source in a separate directory; publication does not rebuild
it or execute arbitrary code from the evidence document. Verify the checksum again
immediately before upload. A signature alone does not establish source identity; official
workflow provenance and the manifest are required too.

Reruns reuse these exact inputs and resolved identities. Re-fetching bytes does not permit
selecting a newer run, artifact, evidence SHA, moved source tag or different APK.
An already published tag/asset may be verified as an idempotent no-op only when identity
and bytes match; do not rewrite its notes with later evidence on a publication rerun.
Never overwrite different bytes. Changed published content requires a new version.
[Rerun semantics](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/re-run-workflows-and-jobs).

### Failure and waiver boundaries

Missing or expired artifacts, inaccessible evidence, invalid/unknown schema, unreviewed
protocol compatibility, moved tags and provenance/signature/version/checksum mismatches
are hard failures, even with a waiver. No lost candidate is replaced automatically with
a rebuild. A replacement candidate must be selected explicitly, with a matching reviewed
active record; changed bytes clear coverage and require fresh tests or a coverage waiver.
Published identity remains immutable regardless.

Only **insufficient qualifying coverage** is waivable, through a non-whitespace reason.
A valid matching record with empty classes can be explicitly waived; null identity or
unreviewed protocol cannot. ABI exclusions and missing/failing cells remain gaps, never
converted to passes. Keep both tag-selected release bars unchanged.

Publication notes retain exact source/tag/version/checksum, candidate run/artifact IDs,
evidence SHA, protocol SHA/review, publisher revision, actual coverage and any waiver
reason. Blocked attempts expose their report in the workflow summary/logs without
creating a Release; an invalid record reports unknown coverage rather than invented
zero or green results. This preserves truthful reporting while publication is blocked.

### Handoff

The canonical protocol, ADR-0008 and Gate Status definition record this amendment;
this ticket holds the lifecycle/input-selection detail. The implementation ticket now
requires candidate-only tag pushes, manual exact-byte promotion, version-5 parsing,
explicit reviewed evidence selection and end-to-end failure/waiver/rerun verification.
No new unspecifiable fog or separate planning decision emerged. The Device Class
assignment decision remains the enforcement ticket's other blocker.
