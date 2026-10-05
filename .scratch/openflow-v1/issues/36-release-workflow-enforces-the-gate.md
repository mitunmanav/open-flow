# Make the release workflow enforce the acceptance gate

Type: task
Status: resolved
Blocked by: none

## What this is

The decision exists — ADR-0008 puts the gate's enforcement inside
`.github/workflows/release.yml`, before publishing, with the bar selected by the tag's own
major version and a `gate_waiver_reason` that must be non-empty. **Nothing implements it
yet.** `release.yml` still triggers on `v*`, runs `./gradlew test`, and publishes.

This ticket is that implementation, and it is a `task` rather than a decision: there is
nothing left to decide about whether the gate gates.

## Why it was blocked on 33 and 35, and what blocks it now

Two reasons, one practical and one substantive, both now discharged — 33 and 35 are resolved.

- **Practical:** both tickets edited `.github/workflows/release.yml`. The gate job belongs on
  top of a settled release pipeline, not inside a conflict with two other changes.
- **Substantive:** the job **cannot be trusted until it has been exercised on a real tag in
  both directions** — a tag that blocks, and a tag that is waived. A tag cannot be published
  until signing works, which is ticket 35.

What blocks it now is
[How do the three behavioral answers select a Device Class?](64-device-class-assignment.md).
Artifact granularity and publication selection are settled: one active signed APK,
explicit reviewed evidence revision, retained candidate bytes and exact-byte promotion.
[How does publication select reviewed gate evidence and the tested APK?](63-select-reviewed-gate-evidence.md)
holds the canonical lifecycle/input plan, with the implementation handoff below.
The class labels still need a repeatable assignment rule before their coverage can mean
the same thing across testers. Keyboard movement remains contextual G12 evidence.

## What to do

1. Write a checker in the house style — `.github/scripts/` already holds `check_docs.py` and
   `check_attribution.py` — that reads the fenced `json` status block under `## Gate status`
   in `docs/quality/acceptance-gate.md` at the evidence revision selected by the follow-up
   decision, and resolves coverage for a given tag and exact artifact identity.
   **The release checker parses that block and nothing else.** The prose around it is not machine input; a
   reworded sentence must not be able to move a gate.
   The block is now `gate_version` **6**, including `required_scenarios`, active APK identity
   protocol revision/review, a defined per-class shape and the class's `hostility`/`config`
   profile — see
   [What counts as a run](48-record-device-abi-in-the-gate.md) and the protocol. **Read the
   version and refuse a version you do not know**, rather than guessing at a shape. Note
   that the section now holds **two** `json` fences: the record is the first one after the
   `## Gate status` heading, and the one under `### The shape of a populated block` is an
   example with placeholder values. Take the first, excluding historical sections too.
   Match `tag`, source `commit`, `version_code` and `apk_sha256` against the selected
   release tag and signed artifact. All child runs inherit that one identity. Do not
   aggregate results from other APKs, even with the same version or source commit.
   Unknown/missing identity cannot qualify coverage for the selected artifact.
   Derive the required ID set and its total from `required_scenarios`, not from a
   hardcoded count or the observed results. Reject a missing, empty or duplicate
   required-ID list. Complete coverage needs a qualifying passing cell for every
   required ID. Missing results are incomplete coverage; unexpected result IDs
   invalidate the record. Equal cardinality with a different ID is not completeness.
2. Add a job to `.github/workflows/release.yml` that fails before `Publish GitHub Release`
   when coverage does not meet the bar the tag's major version implies: `v0.y.z` needs all
   required scenarios green on one class, `v1.0.0` needs all three.
3. Treat an empty `classes` as a block for **both** bars. Absence of evidence is not a pass,
   and right now it is the actual state.
4. **Enforce the ABI rule, and read the shipped list from the build.** A cell whose `abi` is
   not one of `arm64-v8a`, `armeabi-v7a`, `x86_64` **does not count toward coverage** — and
   neither does a cell with no `abi` at all, or one whose value you do not recognise. Read
   the list from `abiFilters` in `app/build.gradle.kts` rather than hardcoding it, so the
   parser cannot drift from what the artifact actually contains. Note what this is *not*: a
   scenario is never failed for its ABI, only excluded from the cell. Coverage arithmetic
   stays in the status block; this job resolves coverage, it does not re-decide verdicts.
5. Add the `gate_waiver_reason` dispatch input. Non-empty to bypass, and it must land in the
   release notes so a waived release says so where a user can read it.
6. **Do not add it to branch protection.** Tags bypass branch protection entirely, so a
   required status context there would be decorative — the exact failure the map's Notes
   already record once.
7. Publish a results run in the same change if one exists by then; if not, publish no run and
   say so. The gate's own rule is that it carries no invented numbers, and its own release
   must not be the first place that is broken.
8. Add the settled documentation-validation rule to `check_docs.py`: read the live
   required-ID list and compare it with the table first-column IDs under “The scenarios”
   in the same protocol. Reject missing/empty/duplicate registries, duplicate table IDs,
   and any set mismatch. Validate identities, not just counts. Ignore the populated
   example fence and historical prose numbers. Keep the existing required documentation
   check unconditional on PRs; this does not require scanning the local tracker.
9. Apply the settled candidate/evidence input-selection policy in
   [How does publication select reviewed gate evidence and the tested APK?](63-select-reviewed-gate-evidence.md). Promote the official signed APK that testers assessed, verifying its
   SHA-256 immediately before publication. A byte-different rebuild needs fresh
   evidence or a recorded waiver; a waiver does not invent green coverage or permit
   overwriting a published APK. Preserve prior results as dated history when changing
   the active artifact, and clear the active results rather than inheriting them.

Use focused verification showing that adding a scenario table row without its registry
entry fails documentation validation, a same-sized set with a replaced ID fails, and
empty/duplicate required IDs fail. A release record missing a required scenario must not
qualify even if another result gives it the same count. Also verify that a dated prose
count, historical record or example-fence edit cannot change release coverage.
Also verify rejection of a different APK checksum with the same source/version and of
evidence for a different tag/source/version. Exercise promotion using the tested bytes
and the follow-up's selected evidence revision; schema consistency alone is not proof
that the publishing workflow uses the right inputs.

## What this ticket cannot settle

It cannot produce a green gate. Coverage is `N/3` device classes until hardware or a
crowdsourced run arrives, and this ticket makes that state *block releases* rather than
quietly pass. That is the intended outcome, and it will look like a problem until it is not.

## Version identity context

[The release version must come from the tag, and it does not](50-release-version-from-tag.md) is resolved. Its Answer is the canonical versioning policy: use the selected tag's deterministic APK name/code and exact source commit, and preserve published identity on rebuild. This constrains Gate Status identity without deciding record granularity. The policy is not yet implemented in the build/workflow.

## Required-scenario context

[Should the scenario count be checked or derived?](57-should-the-scenario-count-be-checked.md)
is resolved. Its Answer owns the registry and documentation-comparison policy.
The metadata is specified in the protocol; neither validator is implemented by that
planning resolution. Record granularity is settled by
[What granularity does a gate record have?](58-what-granularity-does-a-gate-record-have.md).
Selecting reviewed evidence and the tested artifact is settled by
[How does publication select reviewed gate evidence and the tested APK?](63-select-reviewed-gate-evidence.md).

## Device Class context

[Is the keyboard's insets behaviour a fourth Device Class question or a recorded observation?](59-is-the-keyboards-insets-a-fourth-device-class-question.md)
settles keyboard behavior as prose evidence for G12, with its existing pass/fail results
in JSON. There is no fourth class input or schema change.

### Implementing the derived class

[How do the three behavioral answers select a Device Class?](64-device-class-assignment.md)
is resolved. Its Answer is canonical: the checker **computes rather than trusts** —
recompute the class key from `hostility` by the published count and refuse a record whose
key disagrees. Do not re-derive it from brand, model or skin; a checker mapping `One UI` to
`samsung-class` would pass a record the protocol calls invalid.

Three refusals, each for a reason: an entry with **no `hostility`** (its key would be
unfalsifiable), an answer outside `yes`/`no`/`unknown` (the same rule as an unrecognised
`abi`), and **any `unknown`** — that entry carries no class, so it is excluded from coverage
rather than counted, and must not be defaulted either way. Parse version **6**.

## Publication selection handoff

[How does publication select reviewed gate evidence and the tested APK?](63-select-reviewed-gate-evidence.md)
is resolved; its Answer is the canonical executable lifecycle plan. Implement tag-push
candidate creation without publication, 90-day immutable Actions artifacts, manual
promotion from protected main with explicit tag/evidence SHA/run ID/artifact ID, and
exact APK signature/version/source/checksum/provenance checks. Evidence is selected
from reviewed main history independently of APK source. Parse Gate Status version 6,
including protocol_commit and protocol_reviewed; read requirements at the evidence
revision and ABI/version facts from the candidate source/artifact. Waivers cover only
insufficient coverage, never invalid inputs or unreviewed compatibility.

Do not rebuild during promotion or switch inputs on rerun. Reject expired artifacts,
moved tags, mismatched IDs/identity and different published bytes even with a waiver.
Existing identical publication is a no-op, preserving its original notes. Reports include
pinned identities, truthful coverage and any waiver reason; blocked attempts expose
workflow summaries without creating Releases.

Verify candidate-only tag pushes; explicit main dispatch versus branch/tag misuse;
reviewed evidence SHA selection versus moving refs; artifact/run provenance and expiry;
checksum, signer and version mismatches; unreviewed protocol; valid empty coverage
with/without waiver; schema errors with waiver; exact-byte promotion; and immutable
reruns. Substantive protocol changes require fresh active runs, with compatibility
reviewed by the owner rather than guessed by a semantic parser.

## Answer

Implemented in full. The gate now gates, and **it is red**: `classes` is empty, so both
bars block. That is the ticket's own stated intent — this makes the state block releases
rather than quietly pass — and it is the first thing a reader should know.

### What landed

- **`.github/scripts/check_gate.py`** — the release checker. Reads the first ```` ```json ````
  fence under `## Gate status` and nothing else, refuses a `gate_version` it does not know
  (6 only), and resolves coverage for one tag plus one exact artifact.
- **`.github/scripts/test_check_gate.py`** and **`test_check_docs.py`** — 49 tests, wired
  into `docs-check.yml` so they run on every pull request.
- **`check_docs.py`** gained `check_gate_scenarios`: the required-ID registry against the
  scenario tables, by identity rather than by count.
- **`.github/workflows/release.yml`** rewritten around ticket 63's lifecycle: a tag push
  builds a **candidate only**; publication is a manual promotion from `main` with pinned
  evidence SHA, run ID and artifact ID, and a `gate` job that runs before `publish` can
  start.
- **`app/build.gradle.kts`** — ticket 50's version policy, which was a hard prerequisite and
  unowned. See below.

### The two findings worth more than the code

**A gate with a `publish` job that does not depend on the gate job.** The first draft of the
workflow had `gate` and `publish` both `needs: select`, which would have published on a
failed gate — the exact decorative-check failure this map has now recorded seven times. It
is now `needs: [select, gate]`, and that line is the load-bearing one.

**The populated example in the protocol carried `gate_version` 5 while the live record and
ADR-0008 said 6.** Ticket 48 wrote the shape, ticket 64 bumped the live block to 6 and the
prose, and the example fence was missed. Since a tester fills a block **by hand from that
shape**, the example was teaching a version the checker refuses. Sixth instance of the
map's "an amendment that touches a machine-read format has to say which part is the
contract" family, and the first one where the drift was in a document a *human* copies
rather than a parser reads.

### The ABI list is read from the build, and that had a consequence

`read_shipped_abis` parses `abiFilters` out of `app/build.gradle.kts` and fails when the
list is absent or empty. There is deliberately no fallback list: a hardcoded copy would be
a second statement of ADR-0010's fact, and this repository's experience is that the second
statement goes stale silently. Verified by a test that feeds the parser a narrowed list and
watches a previously-qualifying `x86_64` cell stop qualifying with no code change.

### Ticket 50's implementation came with it, and it was unowned

The checker has to match `version_code` against the tag, which is impossible while the build
hardcodes `1` / `0.1.0`. Ticket 50 handed its implementation to "the build and publishing
workflow" and named no owner; 36 was the only ticket that could not be finished without it.
Implemented as `-Popenflow.releaseTag`, with `requireReleaseVersion` refusing any release
build without one — **including the unsigned build CI uses to measure the APK's size**,
because permitting an unsigned artifact and saying which version it is are separate
permissions. Measured: `v0.2.1` → code 2001, `v0.0.1` → code 1. Four refusals verified
(missing tag, `-rc1` suffix, a component above 999, `v0.0.0`).

### One thing I tried and dropped

Renaming the APK after the tag, so a downloaded file is recognisable. `outputFileName` does
not exist in AGP 8.13's public variant API — the only route is the deprecated
`applicationVariants` output API, which is slated for removal in AGP 9. Not worth a
deprecated surface on a repo that has just moved to AGP 8.13, and a filename is not identity
anyway: the manifest carries the tag, source commit, version and SHA-256. The workflow now
asserts there is **exactly one** release APK and globs for it.

### Three judgement calls the ticket did not specify

Each is in the script's comments; recording them here so a later reader knows they were
choices rather than derivations.

1. **A cell with fewer than three runs does not qualify.** Two passes out of two is not two
   of three. Treated as a coverage gap (waivable), not an invalid record.
2. **A fourth run invalidates the record.** The protocol says a fourth run is a second cell,
   so it is a mistake in the record rather than a gap.
3. **A run result that is neither `pass` nor `fail` invalidates the record.** Not excluded
   like a bad `abi`: a cell is a hand-typed structure and an uninterpretable value is
   exactly where a tolerant check passes quietly.

### What this ticket could not do, said plainly

**The gate has never gated a release.** No tag exists, so no candidate has been built by
CI, no artifact has been promoted, and the promotion path — evidence-SHA selection, run/artifact
provenance, expiry, exact-byte promotion, the idempotent-republish no-op — is entirely
unexercised. What is verified is the checker (49 tests plus an end-to-end run against a
real built APK and the real empty record) and the Gradle refusals. The map's own standing
lesson applies with full force: *verifying that a job runs is not verifying that it gates*,
and this is the first release in the project's history that will exercise a gate nobody has
watched fire. The protocol and ADR-0008 both now say so in those words rather than leaving
it implied.

### No results run was published

The ticket's step 7 offered a results run if one existed. `classes` is empty, so there is no
result to publish, and publishing none is the correct outcome rather than a gap.

### Fog

Nothing graduated. The only sharp question this surfaced — whether the waiver path is
reachable in the repository's current state — turned out to be settled by ticket 63 already
("a valid matching record with empty classes can be explicitly waived; null identity or
unreviewed protocol cannot"). It is now stated in the protocol and `CONTRIBUTING.md`,
because a publisher will hit it before they can release anything and would otherwise read
it as a broken waiver.
