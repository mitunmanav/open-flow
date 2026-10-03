# Make the release workflow enforce the acceptance gate

Type: task
Status: open
Blocked by: 33, 35

## What this is

The decision exists — ADR-0008 puts the gate's enforcement inside
`.github/workflows/release.yml`, before publishing, with the bar selected by the tag's own
major version and a `gate_waiver_reason` that must be non-empty. **Nothing implements it
yet.** `release.yml` still triggers on `v*`, runs `./gradlew test`, and publishes.

This ticket is that implementation, and it is a `task` rather than a decision: there is
nothing left to decide about whether the gate gates.

## Why it is blocked on 33 and 35

Two reasons, one practical and one substantive.

- **Practical:** both tickets edit `.github/workflows/release.yml`. The gate job belongs on
  top of a settled release pipeline, not inside a conflict with two other changes.
- **Substantive:** the job **cannot be trusted until it has been exercised on a real tag in
  both directions** — a tag that blocks, and a tag that is waived — and a real tag cannot be
  published until signing works. That is ticket 35. Exercising the trigger is not the same as
  exercising the gate; a job that runs and reports nothing gates nothing.

## What to do

1. Write a checker in the house style — `.github/scripts/` already holds `check_docs.py` and
   `check_attribution.py` — that reads the fenced `json` status block under `## Gate status`
   in `docs/quality/acceptance-gate.md` and resolves the coverage for a given tag.
   **Parse that block and nothing else.** The prose around it is not machine input; a
   reworded sentence must not be able to move a gate.
2. Add a job to `.github/workflows/release.yml` that fails before `Publish GitHub Release`
   when coverage does not meet the bar the tag's major version implies: `v0.y.z` needs all
   14 scenarios green on one class, `v1.0.0` needs all three.
3. Treat an empty `classes` as a block for **both** bars. Absence of evidence is not a pass,
   and right now it is the actual state.
4. Add the `gate_waiver_reason` dispatch input. Non-empty to bypass, and it must land in the
   release notes so a waived release says so where a user can read it.
5. **Do not add it to branch protection.** Tags bypass branch protection entirely, so a
   required status context there would be decorative — the exact failure the map's Notes
   already record once.
6. Publish a results run in the same change if one exists by then; if not, publish no run and
   say so. The gate's own rule is that it carries no invented numbers, and its own release
   must not be the first place that is broken.

## What this ticket cannot settle

It cannot produce a green gate. Coverage is `N/3` device classes until hardware or a
crowdsourced run arrives, and this ticket makes that state *block releases* rather than
quietly pass. That is the intended outcome, and it will look like a problem until it is not.