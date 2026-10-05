# What granularity does a gate record have?

Type: grilling
Status: resolved
Blocked by: none

The resolution constrains
[Make the release workflow enforce the acceptance gate](36-release-workflow-enforces-the-gate.md).
Its remaining input-selection dependency is
[How does publication select reviewed gate evidence and the tested APK?](63-select-reviewed-gate-evidence.md).

## Question

At charting, the gate's status block carried **one** `commit` and **one** `version_code`,
at the top level, for the record as a whole. The original protocol said:

> Every run records its **build identity**: `versionCode` and the commit SHA. A run without
> it cannot be reproduced or attributed.

The original question assumed those could not both be true; the fact check below
corrects that premise. The motivating case was **crowdsourcing**. ADR-0008 calls a crowdsourced run the sanctioned
route out of `N/3` classes, and external testers run **released APKs**. So one canonical
record is going to fold together runs from several people, on several devices, on several
builds — potentially three different `versionCode`s, since a tester on a Pixel and a tester
on a Xiaomi in the same week may be on different releases. A single top-level
`version_code` cannot describe that faithfully. It would name one of the three and imply
the other two runs happened against a build nobody recorded.

Ticket 48 added the ABI per scenario cell and deliberately stopped there: whether the ABI is
per cell is settled, but *where build identity lives* is a question about what a record
**means**, not a field to add, and answering it inside a protocol amendment would have been
a decision taken in the wrong place.

The shapes:

- **One record per build.** `commit` and `version_code` stay top-level and mean what they
  say; a record spanning two builds is two records, and coverage is summed across records
  by the checker. Faithful, and it makes "which build was this class run on?" answerable.
  The cost is that coverage is no longer one number in one block, which is the property
  ADR-0008's Decision explicitly bought — *"One canonical record, with a machine-readable
  status block embedded in it so coverage and prose cannot drift."* Two blocks can drift.
- **Build identity per class, or per cell.** The block keeps its shape and gains the fields
  where they are true. Faithful too, and it keeps one block. The cost is hand-fill burden
  on a form a tester types by ticket — ticket 48's argument for keeping the shape small was
  exactly that — and a record where `version_code` appears in every scenario cell is a
  record nobody fills in correctly.
- **The block records the build *under test*, and per-run provenance stays in the prose.**
  Which is what the current shape half-implies. Cheap, honest if stated — but it means a
  class's green scenarios can come from a build the block does not name, so "reproducible"
  is true of the record and not of every run in it.

Whatever this settles has to say which of the three it is, and ADR-0008's Consequences and
GLOSSARY.md's Gate Status entry both currently imply the third while the prose claims the
second.

Related and *not* to be re-litigated here: ticket
[50](50-release-version-from-tag.md) owns deriving `versionCode` from the tag and owns the
rule that a rebuild of a tag must not move it. This ticket is only about where identity sits
inside a record. If 50 resolves first, its answer constrains this one.

## Comments

### 2026-10-05 — Facts checked before discussion

The question's “cannot both be true” premise is too strong. Every run can inherit
commit/version identity from a homogeneous parent record. Crowdsourcing allows
several people to test the same build; pooling evidence from different builds is a
separate policy choice, not a necessary consequence of crowdsourcing.

The protocol explicitly requires external testers to run a released APK, while the
release gate is intended to precede publishing. Demanding coverage of the prospective
build therefore exposes a bootstrap question. The existing explicit waiver is an
escape for missing evidence, not a way to mark untested coverage green.

[The release version must come from the tag, and it does not](50-release-version-from-tag.md)
requires exact selected-tag source and immutable published APK bytes, and distinguishes
ordinary debug defaults from release identity. These constraints remain in force.
The first decision round asks whether coverage is build-specific and whether officially
supplied signed candidate APKs may be tested before publication, amending the current
released-only rule. Representation and record-loading consequences follow those choices.

### 2026-10-05 — First round accepted

The owner accepted both recommendations:

- Release coverage applies to the specific APK assessed. Older-build results remain
  historical and do not fill coverage gaps for a newer build. Runs may inherit a
  shared build identity once.
- External testers may use an official signed candidate before publication, recording
  its source commit, version code and APK SHA-256. This amends the released-APK-only
  restriction; it does not make ordinary debug defaults release evidence.

Remaining structure choices: one active artifact record versus multiple machine-readable
build entries, and how publication preserves the tested artifact's byte identity.

Fact for the eventual workflow handoff: committing test results changes the repository
commit. The tested APK's source commit and the revision containing reviewed evidence
must be distinguished; reading only the selected tag's embedded document freezes evidence
at that tag. How the publishing workflow loads later reviewed evidence is now a sharp
follow-up question, to be ticketed when this schema decision is recorded.

## Answer

Resolved through two live rounds on 2026-10-05. The owner accepted all recommendations,
confirming the shared understanding when the second round settled the record structure
and publication behavior.

### One active APK, shared identity

The one live Gate Status JSON describes one specific signed APK. Record its intended
release `tag`, full source `commit`, deterministic `version_code` and `apk_sha256` once
at the top level. Every class, scenario cell and run inherits that exact identity.
The source commit is the one used to build the APK, not the later commit recording its
test results. Source/version alone cannot identify exact bytes, so the signed APK's
SHA-256 is required. No per-run identity duplication or cross-build aggregation is added.

Keep one canonical document. When selecting a different APK, preserve earlier results
in dated historical sections, with their APK identity and original requirement context,
and clear active results. Historical evidence is excluded from release validation.
Results for a different APK never fill gaps, even when source or version matches.

The original ticket overstated a contradiction: all runs can inherit an identity from
a homogeneous parent record. Crowdsourcing means several testers can test the same APK;
it does not require pooling green results from several builds.

### Candidate testing and publication

External testers may test an official signed candidate before publication, or a published
release APK, with its source/version/checksum identity supplied. This amends the earlier
released-APK-only restriction. Ordinary debug defaults are not release-gate evidence;
the deterministic version policy and immutable published identity remain in force.

Publication promotes the exact signed APK tested, with a SHA-256 check immediately
before publishing. A rebuilt APK with different bytes needs fresh evidence or an explicit
public waiver; prior coverage does not transfer. A waiver permits publication with the
coverage gap stated, never invents a green gate, and does not allow replacing a published
APK with different bytes.

### Schema and preserved rules

The protocol is now `gate_version` 4, adding `tag` and `apk_sha256` and defining the existing
source/version fields as shared identity for the active artifact. With no active APK or
results, identity fields remain null and `classes` is empty; that qualifies for neither bar.
The populated example is illustrative, not evidence.

Keep the required-scenario registry from
[Should the scenario count be checked or derived?](57-should-the-scenario-count-be-checked.md),
the exact required-ID completeness rule, the three runs per cell on one device,
two-of-three passing, and ABI exclusions. The two release bars remain unchanged.
The release checker reads only the active JSON, excluding examples and historical records.

### Handoff and new frontier

ADR-0008 and the protocol record this policy; GLOSSARY.md now defines Gate Status as
artifact-specific. The implementation handoff in
[Make the release workflow enforce the acceptance gate](36-release-workflow-enforces-the-gate.md)
includes active identity matching, exact-byte promotion and relevant mismatch checks.
No production workflow or validator is implemented by this planning resolution.

Results committed after testing create a new evidence revision. The selected source
tag's embedded document cannot see those later results. Selection of reviewed evidence
and the retained candidate bytes is now a precise decision owned by
[How does publication select reviewed gate evidence and the tested APK?](63-select-reviewed-gate-evidence.md),
which replaces this ticket as the remaining blocker of the enforcement work. That ticket
is created and wired, not resolved here. The unrelated recovery-surface fog remains open.

## Version identity context

[The release version must come from the tag, and it does not](50-release-version-from-tag.md) is resolved. Its Answer is the canonical versioning policy: use the selected tag's deterministic APK name/code and exact source commit, and preserve published identity on rebuild. This constrains Gate Status identity without deciding record granularity. The policy is not yet implemented in the build/workflow.

## Required-scenario context

[Should the scenario count be checked or derived?](57-should-the-scenario-count-be-checked.md)
settled a `required_scenarios` registry in the live JSON and exact documentation
validation against the scenario tables. That decision introduced `gate_version` 3.
This resolution preserves the registry and JSON-only release contract, and advances
the version to 4 for active APK identity.
