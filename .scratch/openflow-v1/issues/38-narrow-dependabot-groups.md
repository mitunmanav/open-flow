# Narrow the Dependabot groups so major bumps arrive alone

Type: task
Status: open
Blocked by: none

## Question

The `actions` group in `.github/dependabot.yml` is `patterns: ['*']`, so all eight
GitHub Actions bumps land in one pull request. PR #1 is the demonstration: **eight major
bumps**, including `actions/checkout` v4 → v7, `actions/setup-java` v4 → v6 and
`actions/upload-artifact` v4 → v7, touching `.github/workflows/release.yml` and
`.github/workflows/pages.yml`.

That matters because of ADR-0009. The landing procedure decided in ticket 29 is the owner
re-authoring each bump by hand, which only stays honest while the owner reads the
changelog. A weekly bundle of eight breaking changes into the release and Pages workflows
trains the owner to stop reading — and the whole reason automation is kept out of
authorship is that a person is accountable for what lands.

Split the groups so a major bump arrives in its own pull request, and close PR #1.

## What to do

- **Separate major from routine in the `actions` group.** Dependabot groups support
  `update-types`, so the shape is one group keyed on
  `version-update:semver-major` and another for the rest. Verify the accepted values
  against Dependabot's current docs rather than trusting this sentence — an invalid
  `update-types` value may be ignored silently, which would leave the config looking
  fixed and changing nothing.
- **Check whether the `gradle` ecosystem needs the same treatment** once it produces its
  first PR. It is live now: `gradlew` and `gradle/libs.versions.toml` exist since ticket
  27, so the `gradle` block is no longer aspirational, and a major AGP or Kotlin bump is
  at least as consequential as a GitHub Actions one.
- **Close PR #1** with a message pointing at ADR-0009 and this ticket, so the next person
  to see a red Dependabot PR knows it is a landing-path question and not a malfunction.
  Do not land it as-is: eight majors into the workflows that build and publish the release.
- **Fix the two stale comments in the same file while you are there.** Line 9-10 says
  *"Once gradlew exists this becomes the main source of dependency updates"* — it exists
  now. Lines 50-51 say *"OpenFlow has no JavaScript or Python build"* next to a live
  `gradle` block, which reads as self-contradictory to anyone who opens the file. Per the
  map's standing rule, a document that no longer matches reality is a bug.

## Notes for the session

- `docs-check` enforces the no-stale-documents rule as a ratchet, so comment edits here
  are welcome but a *link* to something nonexistent would fail the gate.
- Do not add an exemption to `check_attribution.py` while in this area. ADR-0009 decided
  the check needs none, and the trailing-bump PRs are the thing that keeps it that way.
- After narrowing, the next PR that arrives is the natural first exercise for
  `.scratch/openflow-v1/issues/39-dependabot-landing-procedure.md`. Nothing blocks 39;
  this one simply reduces the blast radius of whatever lands first.