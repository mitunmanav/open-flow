# 0009: How an automated dependency bump reaches `main`

Date: 2026-10-03

## Status

Accepted

## Context

`AGENTS.md` states two separable things: automation must never commit to this
repository's branches, and no bot may be a commit author or appear in a
`Co-authored-by` trailer. Ticket 23 enabled Dependabot without deciding how its pull
requests were meant to land. Ticket 18 then made `attribution` a required check, which
turned that silence into a queue of permanently red pull requests.

Nothing is broken. `.github/scripts/check_attribution.py` is doing exactly what it was
written to do, and its own failure message says so — *"Dependabot PRs are welcome; the
bot's commits must not be merged into main as-is."* The undecided thing was the landing
path.

The check turned out to be stricter than the rule it enforces, in three specific ways
that had to be settled before any landing path could be chosen.

**It polices `Signed-off-by`, which `AGENTS.md` never mentions.** R1's stated rationale
is *"GitHub counts trailer names as contributors"*. That is true of `Co-authored-by` and
false of `Signed-off-by`, which is a DCO sign-off and contributes to nobody's graph. So
one of the seven trailer keys it matches is policed on a premise that does not hold.

**It cannot see provenance at all.** Its only input is `git log`. There is no notion of a
pull request, a dependency file, or a cherry-pick, so the landed commit's metadata is the
entire question.

**No GitHub-native merge can land a bot's pull request.** Squash-merge attributes the
squash to the pull request's author — which is why pull requests #2 and #3 landed on
`main` as `Mitun Manav G Y`, not as `GitHub <noreply@github.com>` — so a Dependabot
squash would be authored by `dependabot[bot]` and fail R3 and R2. Rebase-and-merge
preserves the bot's authorship and also fails. The only route that works is a local,
owner-authored commit carrying the bot's diff.

That route has a trap in it. `git cherry-pick` preserves the *original author*, so the
obvious implementation of "re-author by hand" produces exactly the commit the check
exists to reject. Cherry-pick also copies the message verbatim, and Dependabot's message
ends in `Signed-off-by: dependabot[bot] <support@github.com>`.

That trailer is worth being precise about, because it is easy to assume otherwise.
Dependabot appends it to commits it authors itself. GitHub's web-UI squash appends
nothing at all: pull requests #2 and #3 landed on `main` with `GitHub <noreply@github.com>`
as committer and zero GitHub-added trailers, and the `Co-authored-by: Mitun Manav G Y`
line inside commit 2 came from the pull request body, which the squash absorbed. So this
is a bot signing its own work, not machinery stamping metadata — which is why it is not
covered by the existing `GitHub <noreply@github.com>` committer exemption.

## Decision

- **Automation proposes; a human authors; a human merges.** The rule binds the landed
  commit's attribution, not the diff's provenance. This is the reading `AGENTS.md`'s own
  text supports, and it is the only reading under which the rule and Dependabot coexist
  without a carve-out.
- **The policy is general, with Dependabot as the worked example.** `AGENTS.md` names
  `github-actions[bot]` alongside `dependabot`, and this repository already has automation
  committing releases and Pages deploys through Actions.
- **Dependabot stays.** A dependency floor maintained a few times a month is cheap; the
  alternative is floors set by hand and drifting silently.
- **`Signed-off-by` stays policed, and the bot's sign-off is stripped on re-author.** The
  owner becomes the sign-off, so `Signed-off-by: dependabot[bot]` on an owner-authored
  commit is a claim that did not happen. `check_attribution.py` gains no exemption and no
  change.
- **A non-authorship `Refs:` pointer is required** on the landed commit. GitHub links a
  squash-merged pull request as `(#7)` in the subject for free; the manual route loses
  that link, so without an explicit pointer the audit trail for every dependency bump
  disappears the moment the landing path becomes manual.
- **A script performs the re-author and verifies locally before any push**, with the
  procedure documented alongside it. `enforce_admins` is on and the repository is
  solo-maintained, so a remotely-red run has no cheap escape hatch while a local one costs
  a minute. This check has already produced one false positive that made the repository
  unmergeable.
- **No security-fix carve-out.** `automated-security-fixes` arrives as
  `github-actions[bot]` pull requests too, so a critical CVE waits for the owner to wake
  up. Ticket 28's `gate_waiver_reason` precedent deliberately does *not* transfer: a
  skipped acceptance gate is recoverable, a bot-authored commit in `main`'s history is
  not.
- **The `actions` dependency group is narrowed** so major bumps arrive separately from
  routine ones. A weekly bundle of eight breaking CI changes trains the owner to stop
  reading, which defeats the reason automation is kept out of authorship.

## Why these

**Attribution rather than provenance.** A provenance rule — no bot's work reaches `main`
in any form — would make "re-author by hand" a violation rather than a solution, and it
would leave the project choosing between the rule and its dependency floor. Nothing in
this repository's history requires it: `main` is already clean, ten commits with no bot
author among them.

**The sign-off stripped rather than exempted.** Exempting it would have been defensible if
the trailer were inert. It is not — the bot really is signing. And because the landed
commit keeps Dependabot's message body for the `Refs:` audit trail, the trailer comes
along whether or not anyone intends it to. Stripping one line costs nothing and keeps the
rule absolute. The alternative was an exemption bought to accommodate a rationale error,
which is a bad trade in a check that has already been wrong once.

**No security carve-out.** The latency being bought away is minutes of commands, not a
review cycle. An exemption surface introduced for emergencies is an exemption surface
that eventually gets used on a Tuesday.

**Majors split from routine.** Grouping is the mechanism that makes the landing procedure
sustainable rather than ceremonial. Left as one weekly bundle of `patterns: ['*']`, the
procedure's first outing would also be its least careful, on `actions/checkout` v4 → v7
and `upload-artifact` v4 → v7, both of which carry breaking changes into
`.github/workflows/release.yml` and `.github/workflows/pages.yml`.

**Rejected: teaching `attribution` a bot-author exemption on dependency pull requests.**
This weakens the rule at exactly the point `AGENTS.md` draws it, and the resulting bot
commit is permanent in the contributor graph.

**Rejected: dropping Dependabot** for a scheduled read-only report. Honest about the cost
and it loses the automation, and it would have been taken here for the wrong reason — to
avoid deciding, not because the dependency floor stopped mattering.

**Rejected: an auto-merge allowlist.** Smallest human cost, largest rule surface, and it
interacts badly with `enforce_admins` plus linear history.

## Consequences

- **`main` needs no history repair.** All ten commits are owner-authored; the two
  web-UI squashes are attributed to the owner's name and pass R2 on name match alone.
- **The routine is now several commands per bump, performed by a person.** That is the
  accepted cost, and it is paid in minutes rather than review cycles.
- **A bot-authored commit can still be pushed to an open branch** and simply will not
  merge. The check is a merge gate, not a branch guard.
- **`Signed-off-by` remains policed even though its R1 rationale is wrong.** Fixing the
  rationale is not needed for correctness here and is deliberately not bundled into this
  decision; the ticket that narrows the dependency group is the place to revisit it.
- **The script is the documented path, not an optional convenience.** A procedure that
  works only when remembered is not a landing path.
- **The script exists, at `.github/scripts/land_dependency_pr.py`, with the procedure in
  `CONTRIBUTING.md` under "Landing a Dependabot bump".** Two things it does that this
  decision did not anticipate, both narrowing rather than widening. It refuses a
  source that is **not** automation-authored, because a script whose purpose is "make
  this commit pass attribution" would otherwise launder a collaborator's commit into
  the owner's name — R2's purpose inverted. And a **major** bump additionally requires
  `--read-changelog`, which is a claim the script cannot check: the accountability
  argument above is only worth anything if the procedure says where to discharge it.
- **Landing is a new pull request from your own branch, then closing the bot's** — not
  a force-push onto Dependabot's branch. A squash takes its author from the pull
  request, not from the commit, so merging the bot's pull request attributes
  `dependabot[bot]` however the commit underneath is signed. This is why the `Refs:`
  pointer above is load-bearing rather than tidy: it is the only surviving link to the
  bot's pull request once the landing path is manual.