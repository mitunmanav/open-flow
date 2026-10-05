# Contributing

Thanks for considering it. OpenFlow is pre-alpha, so the most useful
contributions right now are architecture discussion, bug reports from real
devices, and provider work.

1. Open an issue before large work.
2. Keep the dependency rule: `app → core ← providers/*` (`docs/adr/0005-module-layout.md`).
3. New providers implement the `SpeechProvider` contract (`docs/adr/0001-speech-provider-contract.md`) and register in one place.
4. `./gradlew lint test assembleDebug` must pass; provider changes need contract tests.
5. Never commit keystores, keys, or provider API secrets.

## Building

Needs **JDK 17** and an Android SDK with **platform 36** — `ANDROID_HOME`, or
`sdk.dir` in a gitignored `local.properties`. No Android Studio, no NDK: the
sherpa-onnx dependency is a prebuilt AAR that already contains its native libraries
for all four ABIs.

```sh
./gradlew lint test assembleDebug   # the gate rule 4 asks for
./gradlew test                      # unit tests only
./gradlew :app:installDebug         # onto a connected device
```

Use the committed wrapper; do not substitute a locally installed Gradle. Every
version the build depends on is declared once in `gradle/libs.versions.toml`, and the
reasoning behind those choices is in
[`docs/adr/0007-build-toolchain-and-sdk-levels.md`](docs/adr/0007-build-toolchain-and-sdk-levels.md).

Be precise about what that gate currently proves: it compiles, lints and assembles,
and it confirms the sherpa-onnx AAR is consumable — but it runs **no tests**, because
there is no application code yet. The first real test arrives with the first real
feature.

## Release builds

A release needs two things the build will not do without: [Signing
Material](GLOSSARY.md) and a **release tag**. `assembleRelease`, `packageRelease`
and `bundleRelease` fail without both, rather than producing an artifact nobody
can identify. The signing reasoning is in
[`docs/adr/0012-release-signing.md`](docs/adr/0012-release-signing.md); the short
version is that Android refuses to upgrade an app whose signature changes, so an
artifact signed by the wrong key cannot be walked back. The version reasoning is
that an artifact whose `versionCode` and `versionName` are hardcoded makes the tag
and the artifact independent, so tagging `v0.2.0` publishes something claiming to
be `0.1.0`.

```sh
./gradlew :app:assembleRelease \
  -Popenflow.releaseTag=v0.2.0 \
  -Popenflow.signingProperties=/path/to/signing.properties
```

The tag is `vMAJOR.MINOR.PATCH` and nothing else — no suffix, no leading zeroes,
no component above 999 — and it sets both version fields: `versionName` is the tag
without its `v`, and `versionCode` is `major * 1000000 + minor * 1000 + patch`.
So `v0.2.1` builds code `2001`, and the acceptance gate recomputes that rather than
trusting a number the build reported about itself. The bounds stop a component
from spilling into the next one's digits, where `v0.1000.0` and `v1.0.0` would
claim the same code.

The properties file sets `storeFile`, `storePassword`, `keyAlias` and
`keyPassword`, and a relative `storeFile` resolves beside the properties file.
The same file at `signing.properties` in the repository root also works — it is
gitignored, as is `*.jks`.

To build a release APK deliberately unsigned, for size measurement or to check
that a release build still assembles:

```sh
./gradlew :app:assembleRelease \
  -Popenflow.allowUnsignedRelease=true \
  -Popenflow.releaseTag=v0.0.1
```

The tag is still required: allowing an unsigned artifact and saying which version
it is are separate permissions, and a release whose version is a default is a
release whose identity nobody chose. That output is named
`app-release-unsigned.apk` and **cannot be installed on a device**. Never publish
one. `.github/workflows/ci.yml` passes exactly that command, and the version in
it is a probe that is never published.

Debug builds need no tag. They use `0.0.0-dev` / `1`, which are not release
identity and are never acceptance-gate evidence.

## Releasing

`.github/workflows/release.yml` does two separate things, and the distinction is
the point of it.

**A tag push creates a candidate. It publishes nothing.** It builds a signed APK
from that exact tag, proves the signer is the release key, and uploads the APK
plus a manifest — tag, source commit, version, SHA-256, signer certificate, run
ID — as an Actions artifact retained for 90 days. That is what a tester installs,
including a tester on a device class this project cannot afford to buy. No
coverage is required, because requiring coverage to hand somebody a test build
would mean the only builds that exist are the ones nobody tested.

**Publication is a manual promotion, dispatched from `main`.** It downloads the
exact bytes a named candidate run produced and enforces the acceptance gate
before creating a Release. To promote:

1. Push a plain `vMAJOR.MINOR.PATCH` tag and let the candidate run finish. Note
   its **run ID** and the candidate artifact's **ID** (not its name) from the run
   summary.
2. Install that candidate on each device you have, run the scenarios in
   [`docs/quality/acceptance-gate.md`](docs/quality/acceptance-gate.md), and
   record the results in its Gate Status block in a pull request. Identity is
   shared by the whole block: tag, source commit, version code, APK SHA-256, and
   the `protocol_commit` you tested under with `protocol_reviewed` set true.
3. Once that evidence PR is on `main`, dispatch the release workflow from `main`
   with `release_tag`, the **full SHA** of the evidence commit,
   `candidate_run_id`, `candidate_artifact_id`, and `gate_waiver_reason` if you
   have one.

The bar comes from the tag's own major version: `v0.y.z` needs every required
scenario green on one device class, `v1.0.0` needs all three. The record's identity
must match the artifact byte for byte, and the checker
(`.github/scripts/check_gate.py`) recomputes the version code from the tag rather
than reading it out of the APK.

**Today it blocks, and the first thing it blocks on is not coverage.** The live
record has null identity and no reviewed protocol, and neither is waivable — a
waiver covers insufficient coverage, not a record that does not identify an
artifact. So the first promotion cannot be unblocked with a reason alone: record
the candidate's `tag`, `commit`, `version_code` and `apk_sha256`, plus the
`protocol_commit` you tested under with `protocol_reviewed` set true, in the Gate
Status block. With an identity and `classes: {}` present, the waiver path works and
the release notes will say the coverage was waived and why. Coverage stays reported
truthfully either way; a waiver never turns an unmeasured device into a measured
one.

A few things the workflow refuses on purpose: promotion dispatched from anywhere
but `main`; an evidence SHA that is a ref rather than a full SHA, or that is not
in `main`'s history; an artifact ID that belongs to a different run than the one
named; an expired artifact; a candidate run that is not a successful tag push of
that tag; and a rebuild whose bytes differ from an already-published APK. A
no-lost-candidate case is never replaced by an automatic rebuild — select a new
candidate and test it.

A re-run reuses the same pinned inputs. Publishing identical bytes over an
existing Release is a no-op that preserves the original notes, so a re-run does
not rewrite history with whatever evidence exists today.

The gate is **not** in branch protection, and adding it there would be decorative:
branch protection requires status contexts on pull requests, and a tag is not a
branch, so it would never see a release. That is also why the first release in
this project's history is the first thing that will exercise a gate nobody has
watched fire.

## Commit attribution

These are enforced by `.github/workflows/attribution.yml`, not by review
comments. A pull request that breaks them is red.

- **The repository owner authors commits.** `main` is the owner's line; a
  collaborator's work arrives as a pull request, which is where review happens.
- **No `Co-authored-by` or `Signed-off-by` trailer may name a bot, an automation
  account, or an AI assistant** — including one that only reviewed or analysed
  the repository. GitHub counts trailer names as contributors, and this
  repository's contributor list is people only. Agents are tools, not
  contributors.
- **Automation does not write to branches here.** Dependabot opening a pull
  request is welcome; its commits must not land on `main` with a bot author.

Human co-authors are not a violation. GitHub appends co-author trailers by
itself when it squashes a multi-author pull request, and that is not this
repository's business to rewrite.

Check your commits before pushing:

```sh
python3 .github/scripts/check_attribution.py --main HEAD~3..HEAD
```

### Landing a Dependabot bump

A bot pull request cannot be merged by any of GitHub's buttons, which is not an
oversight to work around. Squash-merge attributes the squash to the pull
request's *author*, so merging Dependabot's would put `dependabot[bot]` on
`main`; rebase-and-merge preserves the bot author and does the same. And
`git cherry-pick`, the obvious way to re-author by hand, preserves the original
author too — it produces exactly the commit the check exists to reject.

So a bump lands as a local commit you author, carrying the bot's diff:

```sh
python3 .github/scripts/land_dependency_pr.py 42          # a pull request number
python3 .github/scripts/land_dependency_pr.py dependabot/gradle/wrapper-9.x
```

The script fetches the bot's branch, applies its diff with `git merge --squash`
— which stages the change without creating a commit, so there is no authorship
to inherit — writes a fresh message that does not carry Dependabot's own
`Signed-off-by`, adds a `Refs:` pointer back to the source, and then runs
`check_attribution.py --main <base>..HEAD` **before anything is pushed**. It
pushes nothing and opens nothing. `enforce_admins` is on and this repository is
solo-maintained, so a red run on the remote has no cheap escape hatch while a
local one costs a minute.

Three things it will not do, because each would make the rule skippable:

- It refuses a branch that is not automation-authored. Re-authoring a person's
  pull request into the owner's name is the opposite of what rule 2 is for.
- It refuses if `git config user.name`/`user.email` is not the owner, and it
  re-reads the commit it wrote rather than trusting what it asked for.
- There is no flag that skips the check.

**Read the changelog for a major bump.** ADR-0009 keeps automation out of
authorship because a person is answerable for the bump; if you merge a major
version without reading what changed, that is the same as merging it as the bot
with extra steps. The script detects a major bump from Dependabot's own wording
and refuses without `--read-changelog`, which is a claim it cannot check for
you.

Then, by hand:

1. `git show --stat HEAD` — read the diff once.
2. `git push -u origin deps/42`, then open a pull request **from your branch**
   and close Dependabot's. Merging the bot's pull request still fails: the
   squash takes the author from the pull request, not from the commit, so it
   would attribute `dependabot[bot]` however the commit underneath is signed.
   A squash of *your* pull request attributes to you.
3. Merge your pull request through the web UI. The `Refs:` trailer preserves the
   link to the bot's pull request that a squash would have given for free.

The reasoning is in
[`docs/adr/0009-automated-dependency-landing-path.md`](docs/adr/0009-automated-dependency-landing-path.md).

## Documentation

`docs-check` runs on every pull request that touches markdown and fails the
build if a document points at a file that does not exist, names a backticked
path that does not exist, contradicts its own ADR filename, or falls out of
[`docs/README.md`](docs/README.md). A stale document is treated as a bug,
because a reader cannot tell it from a correct one.

```sh
python3 .github/scripts/check_docs.py
```

The gate is a ratchet, not an exemption list: violations may be parked in
`.github/docs-baseline.txt`, one per line, and a baseline entry that stops
matching a real violation fails the build too, so an exemption cannot outlive
the problem it excuses. If you fix a baselined gap, delete its line in the same
commit. The file is currently absent, which means the gate has no exemptions at
all — that is the intended state, not a missing one, and deleting the last entry
is how it got that way.

When you add a document, add it to `docs/README.md` in the same commit. If you
change what something means, update `GLOSSARY.md`; if you change a decision,
amend the ADR in place and note the amendment in its `## Status` section —
ADRs are never renumbered or rewritten silently.

## Decisions before code

OpenFlow decides in the open before it builds. Architecture questions are
resolved as ADRs, and the reasoning is worth more than the outcome: if you
disagree with a decision, argue with the reasoning rather than patching the
implementation.

Write an ADR only when all three are true:

1. It is hard to reverse.
2. It would surprise a future reader without context.
3. It was a real trade-off between genuine alternatives.

One of three is not enough, and most pull requests do not need an ADR.

## Labels and triage

Issues use the five-role vocabulary in
[`docs/agents/triage-labels.md`](docs/agents/triage-labels.md): `needs-triage`,
`needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`, plus area labels
(`android`, `core`, `providers`, `ci`, `design`, `documentation`).

One issue is one visible deliverable. A pull request that needs three unrelated
things is three pull requests, because a review cannot verify three unrelated
things at once.