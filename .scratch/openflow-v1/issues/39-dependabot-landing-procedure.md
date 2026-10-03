# Build the dependency landing procedure ADR-0009 requires

Type: task
Status: open
Blocked by: none

## Question

ADR-0009 settled that an automated dependency bump reaches `main` by being **re-authored
by the owner** — automation proposes, a human authors, a human merges. Nothing implements
that yet. The rule exists as prose in `AGENTS.md`, `CONTRIBUTING.md` and the check's own
failure message, and the actual procedure has to be written down before it is performed
for the first time.

The mechanics are already known, which is why this is a task and not another decision:

- **No GitHub-native merge works.** Squash-merge attributes the squash to the PR's
  *author* — verified on PRs #2 and #3, which landed on `main` as `Mitun Manav G Y` — so a
  Dependabot squash would be bot-authored and fail R3 and R2. Rebase-and-merge preserves
  the bot author and also fails. It has to be a local owner-authored commit.
- **Do not use a plain `git cherry-pick`.** It preserves the *original author*, so it
  produces exactly the commit the check exists to reject.
- **Strip the bot's sign-off.** Dependabot appends `Signed-off-by: dependabot[bot]
  <support@github.com>` to the commits it authors. Keeping the body means keeping that
  line, and on an owner-authored commit it is a claim that did not happen.
- **Add a non-authorship `Refs:` pointer**, naming the source PR. GitHub links a
  squash-merged PR as `(#7)` in the subject for free and the manual route does not, so
  without this the audit trail for every bump disappears.

## What to do

- **A helper script** — `.github/scripts/` is the right home, and the valuable part is
  not the fetch-and-apply. It is running
  `python3 .github/scripts/check_attribution.py --main <base>..HEAD` against the new
  commit **before any push**. `enforce_admins` is on and this is a solo-maintained
  repository, so a remotely-red run has no cheap escape hatch while a local one costs a
  minute. This check has already produced one false positive that made the repository
  unmergeable, so verifying locally is the difference between a minute and a stuck PR.
- **The documented procedure**, with the script as the documented path rather than an
  optional convenience. Include how to read the changelog for a major bump — ADR-0009's
  argument for keeping automation out of authorship is that a person is accountable, and
  a procedure that does not say so would quietly remove that.
- **The rule in `CONTRIBUTING.md`**, which already has an "Automation does not write to
  branches here" section stating the problem without the mechanism. Check
  `.github/PULL_REQUEST_TEMPLATE.md` too — it has a matching checkbox that should point
  at the procedure rather than restating it.
- **Fix the typo while you are in `CONTRIBUTING.md`:** line 52 reads "when it squashing",
  which should be "when it squashes".

## Notes for the session

- The script must not gain the owner a shortcut around the rule. It exists to make the
  slow path reliable, not to make attribution skippable.
- `Signed-off-by` stays in the check's trailer regex. ADR-0009 recorded that its R1
  rationale is wrong — "GitHub counts trailer names as contributors" is true of
  `Co-authored-by` and false of a DCO sign-off — but decided correctness does not need
  that fixed, so it was not bundled. If you are editing the regex anyway, that is the
  moment to reconsider it; otherwise leave it alone.
- Exercise this on a post-narrowing pull request from
  `.scratch/openflow-v1/issues/38-narrow-dependabot-groups.md` rather than on the eight
  majors in PR #1. The first outing should be the careful one.