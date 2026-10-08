# Review follow-ups from the tooling-base landing

Type: task
Status: open
Blocked by: none

## Task

The two-axis review of the tooling-base landing
(`land/v1-tooling-base`, the rebase of `v1-build-out` onto main) found one blocker
and a set of judgement calls. The blocker — the acceptance checker printing a
full coverage numerator for a class whose record never reached the per-scenario
stage — was fixed before the landing, in the same branch. Everything below was
deferred rather than dismissed, grouped here so it is not lost.

## Deferred items

- **`release.yml`'s two copies of the aapt2 badging parse and the candidate
  download.** The workflow is a two-act structure and each act re-derives the
  same values. One shared step (or a composite action) per derivation.
- **`land_dependency_pr.py`'s two remote-slug regexes.** `L160` and `L302` parse
  the same thing with different expressions; one should be the only one.
- **`check_gate.py`'s `evaluate` signature.** The release's identity travels as
  five loose parameters plus a dict (`REQUIRED_TOP_LEVEL` and `render_report`
  enumerate the same four fields again). The Kotlin half of the same rule already
  has `ReleaseVersion(name, code)`; give the checker an equivalent small type.
- **`ThresholdVerdict.kt`'s stringly outcomes.** A verdict is decided by
  regex-parsing display prose. Outcomes want a type, not a `Regex`.
- **`WordErrorRate.kt`'s duplicated normalize-split-filter** for `ref` and `hyp`.
- **`RunReport.kt`'s duplicated row terminator** (`"      }"` vs `"      },"`).
- **`Report.coverage_met`'s one-line delegation** to `evaluate_bar`.
- **`providers/sherpa`'s contract tests.** It ships 15 unit tests and no Contract
  Tests, while `core`'s `testFixtures` is enabled with no sources and no
  consumer. This unblocks when ticket 42's contract lands; do it with 43.
- **`providers/sherpa`'s unused `:core` dependency.** Nothing in the module's
  sources names a `core` type today. Revisit when 42's contract lands — the
  dependency exists for it.
- **Ticket 38's attribution.** The narrowed `dependabot.yml` reached main through
  PR #5, not through the branch whose ticket file describes committing it
  locally. The doc reads as this branch's work; it is not.

## Done before landing, for contrast

- the checker's truthful coverage numerator (`invalid` classes read 0/15);
- the unreachable "fewer than three runs" branch removed, and
  `required_class_bar` wired into `evaluate` so the tested rule is the live rule;
- `release.yml`'s waiver reason passed through the environment, matching the
  two sites that already did;
- `check_docs.py`'s gate-block scan stopped at `###`, matching `check_gate.py`;
- `providers/sherpa`'s dead `androidTest` wiring, phantom test citations, and the
  "the harness does not exist yet" contradiction.
