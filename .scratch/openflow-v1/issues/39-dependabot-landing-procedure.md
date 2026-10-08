# Build the dependency landing procedure ADR-0009 requires

Type: task
Status: resolved
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

## Answer

The procedure exists and is exercised, but **not on a live Dependabot pull request** —
that half is deliberately left as [53](53-exercise-the-landing-procedure.md). What
follows is what was built, what was measured, and the one decision the ticket did not
contain.

### The mechanism is `git merge --squash`, and ADR-0009's trap is real

Measured in a throwaway repo rather than assumed, because both halves of the ADR's
argument are load-bearing:

- `git cherry-pick` on a Dependabot commit **preserves `dependabot[bot]` as author**, and
  the result fails `check_attribution.py --main` exactly as ADR-0009 says. Reproduced.
- `git merge --squash` stages the bot's diff and **creates no commit**, so there is no
  authorship to inherit. It also writes no `MERGE_HEAD`, so there is no half-finished
  merge state to unwind — the cleanup is a `reset`.
- `merge --squash` leaves a `SQUASH_MSG` containing Dependabot's body, **including its
  `Signed-off-by: dependabot[bot] <support@github.com>`**. So the message must be built
  from scratch and passed with `-m`; a plain `git commit` after the squash would carry
  the bot's sign-off into an owner-authored commit. The script re-reads the written
  commit and asks the gate's own R1 question about its trailers rather than grepping for
  one bot's name — a substring check for `dependabot` would pass on a security-fix
  branch signed by `github-actions[bot]`, which is the same claim and the same rule.

### The decision the ticket did not contain: you cannot land *the bot's pull request*

The obvious shape — re-author the commit, force it onto Dependabot's branch, merge that
pull request — **does not work, and cannot be made to work.** A squash takes its author
from the *pull request*, not from the commit, so merging `dependabot/gradle/...` still
attributes `dependabot[bot]` to `main` and still fails R2, however the commit underneath
is signed. Rebase-merge would work after re-authoring, but it depends on the bot branch
staying put, and Dependabot rewrites that branch on every run.

So the landing route is: **branch off the fetched base, commit the bot's diff as yourself,
push your own branch, open your own pull request, close the bot's.** Your pull request's
author is you, so your squash attributes to you.

This is why ADR-0009's `Refs:` pointer is load-bearing rather than tidy. The free `(#n)`
link came from the pull request being merged; once the landing path is manual, that
pointer is the only surviving link to the bot's proposal. A branch-sourced landing (no
pull-request number) gets a commit-URL `Refs:` for the same reason — otherwise naming a
branch instead of a number would quietly produce the one unauditable landing.

### What the script refuses, and why each refusal is a narrowing

The ticket's warning was that the script must not become a shortcut around the rule. It
has no `--force`/`--skip`/`--allow` flag at all, and it refuses:

- **A source that is not automation-authored.** A script whose purpose is "make this
  commit pass attribution", pointed at a collaborator's branch, launders a human's
  commit into the owner's name — R2's purpose inverted. It pushes nothing and opens
  nothing; verification is local, before the irreversible act.
- **A git identity that is not the owner**, checked before the commit and re-checked
  against what git actually wrote afterwards.
- **An existing local branch of the target name.** `checkout -B` would reset it, and the
  script must never be the thing that loses work.
- **A local base ahead of what it fetched**, rather than branching from `origin/main` and
  silently leaving those commits behind.
- **A modified tracked file.** Untracked files are deliberately *not* a refusal: they are
  not staged and cannot land in the commit, so blocking on them would be a false positive
  on the owner's own scratch files. The first version used `git status --porcelain` and
  tripped over its own `__pycache__`.

**Accountability got a flag rather than a sentence.** ADR-0009 keeps automation out of
authorship *because* a person is answerable for the bump; a procedure that did not say
where to discharge that would quietly remove it. A **major** bump is detected from
Dependabot's own "from a to b" wording and refused without `--read-changelog`. The flag
is a claim the script cannot check — deliberately so, because it is the owner asserting
something, not the tool verifying something.

### Evidence

46 assertions against a faithful reproduction (a real bare `origin`, real bot-authored
commits carrying `Signed-off-by`, owner identity pinned), all passing, exercised by hand
in the authoring session: the bot's commit fails the gate; the cherry-picked commit
fails the gate; the script's commit passes it with the diff carried, the bot's sign-off
gone and `Refs:` present; the owner is left **on** the new branch; nothing is pushed;
majors require the flag and minors do not; a human branch, a dirty tracked file, a
wrong identity, an existing branch, a local-ahead base and a conflicting bump are each
refused with the tree left clean; an already-landed bump reports "nothing to land" and
makes no empty commit; `--dry-run` creates nothing. The pull-request-number path was
exercised live against PR #4 and correctly refused it as human-authored, creating
nothing.

**The assertions are not committed.** They exist in the session that wrote the script,
not in `.github/scripts/test_land_dependency_pr.py`, so `unittest discover` cannot
re-run them and nothing in CI fails if the script changes. Committing them as a suite
is [69](69-commit-the-landing-procedure-assertions.md).

Two of those assertions were failing because my *fixture* was wrong, not the script: I
had labelled `8.13 → 9.0` a minor bump. The script was right.

### Not done

- **The live first outing.** Ticket 38's narrowed `dependabot.yml` is committed locally
  but **not on `main`** (`main` is still at `1cee4f6`, which carries the un-narrowed
  config), and no Dependabot pull request is open. So the post-narrowing PR this ticket
  asked for does not exist yet. Graduated to [53](53-exercise-the-landing-procedure.md).
- **`Signed-off-by` left in `TRAILER_RE`.** I was not editing the regex, so ADR-0009's
  decision stands: the rationale is wrong, correctness does not need it fixed.

### One incidental fix

`CONTRIBUTING.md` described `.github/docs-baseline.txt` as a live list. Ticket 24 deleted
that file, so the paragraph was pointing a reader at a file that has not existed for
several tickets — and `docs-check` **cannot** catch it, because `.txt` is not in
`KNOWN_SUFFIXES`. Rewritten to say the file is absent and that this is the intended
state. The "when it squashing" typo is fixed; it had moved to line 82, not 52.

### Files

- `.github/scripts/land_dependency_pr.py` — new
- `CONTRIBUTING.md` — "Landing a Dependabot bump"; typo; baseline paragraph
- `.github/PULL_REQUEST_TEMPLATE.md` — third attribution checkbox points at the procedure
- `docs/adr/0009-automated-dependency-landing-path.md` — Consequences record where the
  script and procedure live, and the two decisions above