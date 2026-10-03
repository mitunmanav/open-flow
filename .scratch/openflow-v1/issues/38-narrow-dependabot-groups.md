# Narrow the Dependabot groups so major bumps arrive alone

Type: task
Status: resolved
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

## Answer

**Done.** `actions` and both `gradle` groups are narrowed to `update-types: [minor, patch]`
with `applies-to: version-updates` stated explicitly, the two stale comments are corrected,
and PR #1 is closed with a comment pointing here and at ADR-0009. `docs-check` passes.

### The ticket's proposed mechanism was invalid, and this is the finding worth keeping

The ticket said to shape the split as "one group keyed on `version-update:semver-major`",
and warned in the next breath that an invalid `update-types` value might be ignored
silently. **The ticket's own example was the invalid value.**

Inside a `groups` block the accepted values are **bare** `major` / `minor` / `patch`. The
`version-update:semver-*` spelling belongs to `allow` and `ignore`, which are separate keys
with separate vocabularies — the reference lists them in different tables, and they are easy
to read past because the strings overlap.

Checked rather than assumed, against the published schema:

```
ticket-38 proposed spelling -> INVALID
    ['updates',1,'groups','actions-major','update-types',0] ::
    'version-update:semver-major' is not one of ['major', 'minor', 'patch']
bare "major" under groups  -> VALID
```

The corrected file validates clean against `schemastore.org/dependabot-2.0`.

### GitHub's own documented shape is simpler still: there is no `-major` group

The options reference's **Example 3** is this exact case, and it does not create a group for
majors at all. It narrows the *existing* group to `minor`/`patch` and lets majors fall
through, because the default behaviour is: *"Any outdated dependencies that do not match a
rule are updated in individual pull requests."*

That is what landed, and it is strictly better than the two-group shape for one specific
reason. The reference also says *"If a dependency matches more than one rule, it's included
in the first group that it matches."* A `-major` group plus a `patterns: ['*']` catch-all
would therefore make correctness depend on an unstated question — whether "matches" is
evaluated on `patterns` alone or on `update-types` too. If it is patterns alone, a
**patch** bump of `actions/checkout` matches the major group first and lands in the
"major" pull request, which is precisely the bundling this ticket exists to remove.
Narrowing the routine group makes the two cases **disjoint**, so declaration order stops
mattering and the ambiguity never has to be resolved. **Prefer a partition that cannot be
misread over one that depends on resolution order.**

### `gradle` needed it too, and two dependencies were already correct

Yes — `gradlew` and the version catalogue are live since ticket 27, and an AGP or Kotlin
major is at least as consequential as a GitHub Actions one. Both groups are narrowed.

Two dependencies are worth naming because they are **already ungrouped**, which is the
outcome wanted for each, and neither needed a rule added:

- **The Gradle wrapper** matches neither `com.android.*`, `androidx.*`, nor
  `org.jetbrains.kotlin*`.
- **The sherpa-onnx AAR** is pinned to a `v`-prefixed tag (`v1.13.8`) rather than plain
  SemVer, so it does not land in a SemVer-keyed group — and a deliberate tag change to a
  50 MB JitPack-only artifact is exactly the kind of bump that should arrive alone.

`dependency-type` under `groups` was **not** an option here: it is supported only by
bundler, composer, mix, maven, npm and pip — not by `gradle` or `github-actions`.

### Nothing is exempted, and ADR-0009's security position is untouched

`applies-to` **defaults to `version-updates`**, so writing it out changes no behaviour — it
is stated so a future reader can see the scoping rather than infer it. The practical
consequence is the one that matters: **security updates are not narrowed.** A
`github-actions[bot]` security PR arrives exactly as before and still has to clear the
manual landing path. ADR-0009 deliberately refused a security carve-out, and nothing here
is one. The config comment says so explicitly, because a future reader narrowing `applies-to`
would be quietly rewriting that decision.

### Found, recorded, not acted on

`cooldown` accepts `semver-major-days` for **Gradle** but **not** for GitHub Actions, which
supports only `default-days`. So a "let majors age before proposing them" lever would work
asymmetrically across the two ecosystems. Not used: this ticket's problem was bundling, not
frequency, and grouping solved the problem that was actually stated.

### What is *not* proven yet — and this map has a lesson about exactly that

The config is schema-valid and parses, and that is **all** that has been checked. Nobody has
watched Dependabot honour it. Per the map's standing rule — *a gate that is never exercised is
worse than no gate, because it looks configured*, now recorded three times over — treat this
as **unexercised until the next scheduled run (Mondays 06:00)** raises a routine action bump
as its own small PR and a major as its own single-dependency PR. Verify the trigger *and*
the outcome then, and that observation is what closes the loop.

Two consequences of the change that nobody has watched either:

- **The eight bumps in PR #1 are not landed, only unbundled.** Dependabot computes groups
  when it opens a PR, so editing this file does not retroactively re-split PR #1. Closing it
  means the next run raises them individually.
- **`open-pull-requests-limit` is now load-bearing where it was not.** It is 5 for `gradle`
  and unset for `github-actions` (default 5). With majors unbundled, more pull requests can
  be open at once, so the limit can now actually be reached. It was previously masked by
  bundling. 5 still looks right for a solo-maintained repo, but it is a real ceiling rather
  than an inert default.

`check_attribution.py` is **untouched and gains no exemption**, per ADR-0009 and ticket 29.
ADR-0009's forward-reference to R1's wrong `Signed-off-by` rationale stays wrong on purpose:
correctness of the code is the gate, accuracy of the comment is its own ticket.

### Generalisable

The ticket instructed the session to verify the `update-types` value because an invalid one
might be silently ignored — and the value it supplied was itself the invalid one. A worked
example in the charter is not a checked fact. When a ticket warns about a silent failure
mode, the warning applies to the ticket's own proposed remedy as much as to the config.

Nothing graduated from fog. No new ticket either: the one live consequence worth watching —
whether Dependabot honours the new grouping — is an observation on this ticket's own work,
not a decision, so it belongs in a future session's verification rather than in a new
question. Ticket 39 is unblocked and is the natural next step, and it was already the
designated first outing for whatever lands after this.
