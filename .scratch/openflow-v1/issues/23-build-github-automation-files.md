# Build .github workflows, issue templates, and repo automation files

Type: task
Status: resolved
Blocked by: none

## Question

Ticket 15 settled the contents of `.github/` but nothing was ever written: the repository is a single docs commit with no `.github/` directory. That gap blocks ticket 18, whose acceptance step is "confirm `release.yml` can consume the secrets" — there is no `release.yml` to confirm.

Build from ticket 15's decision: `.github/workflows/{ci,android-test,release,pages,dependency-review,docs-check}.yml`; `.github/ISSUE_TEMPLATE/{bug,feature,provider-proposal,security}.md` (security via `SECURITY.md`); `.github/PULL_REQUEST_TEMPLATE.md`; labels and board columns from ticket 15.

The **provider-proposal template is the one hard requirement** — ticket 21's `docs/providers/provider-proposal.md` cross-links it as the single source of truth for proposal criteria, so the guide is currently a forward reference to a file that does not exist. `release.yml` is the other: it defines the secret names ticket 18 sets (`OPENFLOW_KEYSTORE_BASE64`, `OPENFLOW_KEYSTORE_PASSWORD`, `OPENFLOW_KEY_ALIAS`, `OPENFLOW_KEY_PASSWORD`), so it should land before or with ticket 18.

Note `SECURITY.md`, `THIRD_PARTY_NOTICES.md`, and `pages.yml` (which expects a `website/` directory) are likewise referenced but not yet created.

## Answer

`.github/` is built. Seven workflows, four issue templates, a PR template, `CODEOWNERS`, `dependabot.yml`, and two dependency-free checkers the workflows call. Labels are live on the repository; the project board is the one step left, because it needs a scope only you can grant.

**Labels — created on the repo.** Ticket 15 said labels come "as listed in the ticket", and the ticket listed none, so the vocabulary was reconstructed rather than transcribed. The five triage roles in `docs/agents/triage-labels.md` are reused verbatim (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`; `wontfix` already existed on the repo). Added six area labels: `android`, `core`, `providers`, `ci`, `design`, `dependencies` — and deliberately **not** `docs`, since GitHub's default set already has `documentation`. Deliberately not added: `wayfinder:*` labels, because this repo's maps are files under `.scratch/`, not GitHub issues, so the labels would have no subject to attach to. The `GLOSSARY.md` domain terms (`bubble`, `router`, `refiner`, …) were considered and dropped: unused labels are the label equivalent of an empty module.

**Seven workflows, not six.** `attribution.yml` is new and answers a standing preference in Notes that had nothing behind it — AGENTS.md says no bots in the contributor list, and until now nothing enforced it.

- `ci.yml` — lint/test/assembleDebug + debug APK artifact. Green from day one: a probe step gates the Gradle steps on `gradlew` existing, so the gate is usable while the Android shell is written and goes live by itself when the wrapper lands. No edit needed at that point.
- `android-test.yml` — emulator instrumented tests. On demand, on `v*` tags, and daily at 05:17 UTC (off the hour, so it does not compete with every other scheduled job on the platform).
- `release.yml` — **the contract for ticket 18.** It names the four secrets and nowhere else does: `OPENFLOW_KEYSTORE_BASE64`, `OPENFLOW_KEYSTORE_PASSWORD`, `OPENFLOW_KEY_ALIAS`, `OPENFLOW_KEY_PASSWORD`. Fails loudly if any is missing, rejects non-semver tags, verifies the produced APK is actually signed with the release key rather than silently falling back to the debug key, and publishes SHA-256 checksums. All three paths were exercised locally.
- `pages.yml` — gated on `website/` existing.
- `dependency-review.yml` — advisory, fails on high severity.
- `docs-check.yml` — the stale-document gate, below.
- `attribution.yml` — R1 no bot/AI in a trailer, R2 owner-only authorship on `main`, R3 no bot commits at all. Deliberately *not* a violation: human co-authors, including the `Co-authored-by` trailers GitHub appends by itself when squashing a multi-author PR.

**"Strictly no stale documents" is enforced, not aspirational.** `.github/scripts/check_docs.py` (stdlib only, runnable locally and in CI) fails on: broken relative links, `http://`, backticked file paths that do not exist, ADR numbering gaps, an ADR whose title/date/status contradicts its filename, a `CHANGELOG.md` that exists but is empty, and any document under `docs/` unreachable from `docs/index`. Four escape hatches exist, each because it removes a *correct* sentence otherwise: text in fenced blocks, path templates (`<effort>`), forward markers ("when run", "if it exists"), and `CamelCase.kt` references, which read as types in third-party libraries.

The ratchet is the part worth keeping. `.github/docs-baseline.txt` holds today's known violations so the gate could land without going red and blocking every PR — and a baseline line that stops matching a real violation **also** fails the build. So an exemption cannot outlive the problem it excuses, and the baseline can only shrink. Verified by simulation: writing the three provider guides makes all four current entries fail as stale.

**The checker found four real defects on first run, which is the argument for it.**

1. `CHANGELOG.md` existed and was empty — a repo with tagged history claiming nothing had happened.
2. No `docs/` index, so nothing enforced that a new document gets linked from anywhere.
3. ADR-0006 named the three provider guides as bare `provider-authoring.md` — not just unwritten, but *unlocatable*. Even after they are written, a reader following that sentence would not find them. Amended in place to `docs/providers/provider-authoring.md`; the decision is unchanged, only its references became locatable.
4. `docs/providers/model-selection.md` promises results in `docs/providers/model-benchmark-results.md` "when run" — the benchmark behind ticket 06's model choice has never been run. Filed as ticket 25.

**Consequence for the map: ticket 21 was falsely resolved.** Its answer claims three guides in `docs/providers/`; only ADR-0006 exists. The decision is sound and stays, but the deliverable was never written — a resolved ticket whose artifact does not exist corrupts Decisions-so-far, which is what other sessions orient by. Corrected in place and filed as ticket 24.

**Not pushed.** `main` carries three tickets' worth of in-flight map work; pushing would activate all seven workflows on a public repo as a side effect of an infrastructure decision. Left for you to push when the map work is clean.

**Left for you (one step).** The board from ticket 15 (`Backlog / Ready / In progress / Verification / Done`) needs a scope this session cannot grant:

```sh
gh auth refresh -s project
```

then the board can be created. Added to ticket 18 alongside branch protection, since both are repo administration and both need you.

**One decision left open on purpose:** whether the docs site exists at all, and in what. `pages.yml` is written and gated, but choosing a generator before the doc set is stable is the same premature-module mistake ADR-0006 avoided with `provider-api`. Filed as ticket 26.