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
itself when it squashing a multi-author pull request, and that is not this
repository's business to rewrite.

Check your commits before pushing:

```sh
python3 .github/scripts/check_attribution.py --main HEAD~3..HEAD
```

## Documentation

`docs-check` runs on every pull request that touches markdown and fails the
build if a document points at a file that does not exist, names a backticked
path that does not exist, contradicts its own ADR filename, or falls out of
[`docs/README.md`](docs/README.md). A stale document is treated as a bug,
because a reader cannot tell it from a correct one.

```sh
python3 .github/scripts/check_docs.py
```

Known gaps are listed in `.github/docs-baseline.txt`, one per line, and the
baseline is a ratchet: a line that stops matching a real violation fails the
build too, so an exemption cannot outlive the problem it excuses. If you fix a
baselined gap, delete its line in the same commit.

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