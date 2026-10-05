## Summary

Six independent units of resolved work from the wayfinder map, one commit each so
`git log` traces which decision produced which file. 64 files, +6573 −108.

```text
├── docs   provider guides (ticket 24)
│   ├── docs/providers/provider-{authoring,testing,proposal}.md
│   └── .github/docs-baseline.txt      deleted — the ratchet now has 0 exemptions
│
├── feat   project site (ticket 26)
│   ├── website/                        4 pages, 2 scripts, 2 self-hosted fonts
│   ├── .github/workflows/pages.yml     + configure-pages, the step it was missing
│   └── .github/scripts/check_docs.py   + site nav & asset-resolution rules
│
├── build  Gradle skeleton (ticket 27)
│   ├── app/ core/ providers/ gradle/ gradlew{,.bat}
│   ├── docs/adr/0007                    toolchain, SDK levels, minSdk 26
│   └── ci · android-test · release      JDK 21 → 17, to match the dev machine
│
├── docs   acceptance gate (ticket 28)
│   ├── docs/quality/acceptance-gate.md  14 scenarios, 3 device classes
│   └── docs/adr/0008                    two bars; the tag's major version picks one
│
├── docs   frontier charting
│   └── tickets 30–37                    4 decisions + 5 execution, previously untracked
│
└── docs   dependency landing path (ticket 29)
    ├── docs/adr/0009
    └── tickets 38–39                    the two follow-ups that decision produced
```

Two of these change behaviour rather than only adding prose, and both were defects:

- **`pages.yml` never deployed.** It was missing `actions/configure-pages`, which is what
  enables Pages and authenticates the deploy — so `deploy-pages` failed with a 404 and the
  site was built and never published.
- **`check_docs.py` was green for the wrong reason.** See below.

## Evidence

**The documentation gate was passing while suppressing four real gaps.** On `main`:

```console
$ python3 .github/scripts/check_docs.py
docs-check: OK — 31 markdown file(s) checked (4 baselined).

$ python3 .github/scripts/check_docs.py --no-baseline
::error file=docs/adr/0006-provider-authoring-no-sdk.md::missing-file-ref: … provider-authoring.md
::error file=docs/adr/0006-provider-authoring-no-sdk.md::missing-file-ref: … provider-testing.md
::error file=docs/adr/0006-provider-authoring-no-sdk.md::missing-file-ref: … provider-proposal.md
::error file=.github/ISSUE_TEMPLATE/provider_proposal.md::missing-file-ref: … provider-proposal.md
docs-check: 4 new documentation violation(s).
```

After — the guides exist and the baseline is gone entirely, not shortened:

```console
$ python3 .github/scripts/check_docs.py
docs-check: OK — 38 markdown file(s) checked.
```

**The new site rules actually catch drift.** Removing one `<li>` from a nav in
`website/index.html`:

```console
::error file=website/index.html::site-nav-footer-mismatch: website/index.html — footer lists
  ['index.html', 'how-it-works.html', 'privacy.html'] but the nav lists
  ['index.html', 'how-it-works.html', 'privacy.html', 'get-involved.html']
docs-check: 1 new documentation violation(s).
```

Restored, `docs-check` back to OK with no residue.

**All three required checks pass at HEAD.**

```console
$ ./gradlew clean lint test assembleDebug
BUILD SUCCESSFUL in 5s
213 actionable tasks: 96 executed, 107 from cache, 10 up-to-date

$ python3 .github/scripts/check_attribution.py --main main..HEAD
check-attribution: OK — 6 commit(s) in 'main..HEAD' clean.
```

Attribution was run **with** `--main`, which is stricter than CI uses: the `pull_request`
leg omits that flag, so R2 (owner-authored on `main`) is not evaluated there. Green under
the stricter mode means the PR path is safe too. Every commit carries the `Co-authored-by`
trailer AGENTS.md requires; no bot is an author or committer.

Note the build executes **zero tests** — there is still no application code. The gate
proves the toolchain assembles and that the sherpa-onnx AAR is consumable. That is stated
in `CONTRIBUTING.md` rather than left to look like more than it is.

## Merge Danger

**Door:** two-way.

Every commit is an ordinary revertible change — no migrations, no deletions of live code,
no schema or wire-format changes. The one addition that escapes the repo is the website.

**Blast Radius:** public.

Worth naming explicitly, because it is the part that is not undone by reverting code:

- **Merging makes GitHub Pages actually deploy.** The workflow existed and was green; it
  was publishing nothing. This PR is what turns a public site on at the repository URL.
  Reverting the `configure-pages` step stops future deploys but does not un-publish what
  has already shipped.
- **ADRs are permanent by this repo's own convention.** `docs/README.md` requires a
  reversal to be recorded as an amendment in the ADR's `## Status` section, "because a
  decision that has been reversed silently is a decision nobody can trust." So ADR-0007,
  0008 and 0009 are append-only in practice.
- **Ticket 28 amends the destination.** The gate is now two bars rather than one, so
  "shipped" means something narrower than the map originally stated. That change is
  recorded in the map's Destination section rather than applied quietly.
- **CI now runs on every PR for real.** `ci` no longer skips, so the Gradle build executes
  rather than probing for files that do not exist.

Two things I did **not** do, so they are not silently bundled here: `CHANGELOG.md` still
has no entry for tickets 28 or 29 and its ADR line still reads `0001–0006`; and tickets
18, 20 and 25 remain in `claimed` with no `## Answer`. Both are follow-ups, not this PR.