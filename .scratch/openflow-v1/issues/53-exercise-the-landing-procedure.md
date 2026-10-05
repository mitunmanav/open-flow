# Exercise the landing procedure on a real Dependabot bump

Type: task
Status: blocked:human
Parked on: Await an eligible Dependabot minor/patch PR. Re-read 2026-10-05: Dependabot **has** run and five pull requests are open, but every one is a **major**, and majors are not eligible. See "Current prerequisite state" and the Comments.
Blocked by: none

## Question

Ticket 39 built and verified the landing procedure against a faithful reproduction.
Nobody has run it on a real Dependabot pull request. This is the first outing, and the
point of it is not the bump — it is finding out whether the reproduction was faithful.

## Current prerequisite state

Re-checked live on **2026-10-05**, superseding the 2026-10-04 reading below:

- The narrowed grouping **is** on `main` at `212d74ba8b0e19fd95000911eb0f464c20af580b`.
  Verified: `git show origin/main:.github/dependabot.yml` carries three
  `applies-to: version-updates` / `update-types: [minor, patch]` groups.
- **Dependabot has run and five pull requests are open.** The 2026-10-04 note that
  "no Dependabot PR is open" is stale. Open bot PRs: #6 `actions/checkout` 4→7,
  #7 `actions/upload-artifact` 4→7, #8 `actions/dependency-review-action` 4→5,
  #9 `softprops/action-gh-release` 2→3, #10 `gradle/actions` 4→6.
- **All five are majors**, confirmed from each commit's own `updated-dependencies`
  block (`update-type: version-update:semver-major`), so **none is eligible.** A major
  is not a routine bump and must not be landed under this ticket's procedure.
- The majors arriving as **five separate pull requests** rather than one bundle is
  ticket 38's narrowing working as designed; PR #1, which bundled eight majors, is
  closed. So the prerequisite this ticket was parked on is genuinely satisfied, and
  the remaining wait is for a minor/patch bump, not for Dependabot to run.
- `origin/main` is clean under `check_attribution.py --main` (11 commits) and the
  contributors API returns only `mitunmanav`.

Checked live on 2026-10-04 (superseded by the above, kept for the record):

- The narrowed grouping is now on `main` at `212d74ba8b0e19fd95000911eb0f464c20af580b`,
  landed independently through
  [Keep major Dependabot updates out of routine groups](https://github.com/mitunmanav/open-flow/pull/5).
- No Dependabot PR is open. The remaining open PR is human-authored and is not eligible.
- The next configured weekly run is Monday 2026-10-05 at 06:00 UTC. The owner may also
  trigger a Dependabot check from GitHub. This ticket cannot complete until a real eligible
  routine bump exists.

The previous assumption that the project-build PR contained the narrowed configuration
was wrong: its live `.github/dependabot.yml` still had broad groups. The isolated
configuration PR removed that prerequisite without merging the larger project-build PR.

## What to do

- Confirm `main` carries ticket 38's narrowed `dependabot.yml` before anything else. If a
  pull request is already open from the un-narrowed config, close it rather than landing
  it — landing it would put the eight majors back in one bundle, which is what ticket 38
  exists to undo.
- On the first routine (minor/patch) bump:
  ```sh
  python3 .github/scripts/land_dependency_pr.py <pr-number> --dry-run
  python3 .github/scripts/land_dependency_pr.py <pr-number>
  ```
- Push the branch, open your **own** pull request from it, close Dependabot's, merge
  yours. Do not merge the bot's — see ticket 39's answer for why that cannot work.
- Confirm the landed commit on `main` carries: the owner as author, a `Refs:` trailer, no
  bot sign-off, and no bot in the contributor graph.

## What would make this ticket worth having

Ticket 39's exercise was a reproduction, and a reproduction is a hypothesis about
reality. Three things it could not check, each of which is a way this could be wrong in a
way nobody would notice:

1. **That Dependabot's actual commit shape is what the fixture assumed** — particularly
   the `Signed-off-by` trailer, which the script's R1 re-read guards against but which may
   arrive formatted differently in a grouped bump.
2. **That `gh pr view`'s fields are populated as expected for a real Dependabot pull
   request.** The `author.is_bot` and `isCrossRepository` checks have only ever seen one
   human pull request.
3. **That the gate agrees.** `check_attribution.py --main` has never been run against a
   re-authored commit that actually came from Dependabot.

A green run retires all three at once. A failure in any of them is a real finding about
the procedure, and belongs in ticket 39's answer as an amendment rather than a quiet fix.

## Notes

- A red Dependabot pull request is **not** a malfunction. The red is the rule working.
- Do not widen the procedure to make an awkward bump easier. The refusals in
  `land_dependency_pr.py` are deliberate narrowings; a bump that cannot pass them is
  information, not an obstacle.

## Comments

### 2026-10-04 — Prerequisite landed; live exercise still pending

Live inspection found `main` at `1cee4f653e2587c2ec384b5e61dad14fdaa31d2d`, broad groups,
no Dependabot PR, and the project-build PR missing the narrowing too.

Prepared a configuration-only branch from `origin/main` in an isolated worktree,
`ci/narrow-dependabot-routine-updates`, and landed
[Keep major Dependabot updates out of routine groups](https://github.com/mitunmanav/open-flow/pull/5).
All three groups now use `applies-to: version-updates` and `update-types: [minor, patch]`.
YAML parsed, the current Dependabot JSON schema validated, all four GitHub checks passed,
and the merged commit passed `check_attribution.py --main` locally. The merged commit
`212d74ba8b0e19fd95000911eb0f464c20af580b` has the pinned owner author identity, a `Refs:`
pointer and owner co-author trailer, with no bot trailer. The contributor API returned
only `mitunmanav` before this owner-authored merge. Fetched `main` was checked again for
all three narrowed groups after merge.

The configured values were rechecked against
[GitHub's Dependabot groups reference](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference).
This proves configuration validity and landing of the prerequisite, not live Dependabot
behaviour or the dependency landing helper. No eligible automation-authored PR exists;
`land_dependency_pr.py` has not been exercised on one, so this ticket remains unresolved
and has no Decisions-so-far entry. No fog graduated.

Next action: when a real minor/patch Dependabot PR arrives, resume this ticket and follow
its dry-run, owner-authored landing, required checks and attribution verification steps.

### 2026-10-05 — Outing run. The procedure was broken; ticket 39's reproduction was not faithful

The park was stale in its reasoning and wrong in its conclusion. Dependabot **had** run and
five bot pull requests were open, so the ticket could be exercised. **All five are majors**,
so none was eligible to land under this ticket — and none was landed. What the outing did
produce is the answer the ticket was actually for: **ticket 39's reproduction was not
faithful, and the script it "verified" could not land a Dependabot pull request at all.**

**Two real bugs, both invisible to the reproduction.** `preflight` read the proposal's tip
with `git log -1 --pretty='%an <%ae>' pull/6/head`, and after `git fetch origin <ref>`
**nothing named `<ref>` resolves** — the fetch writes `FETCH_HEAD` and no local ref:

```
$ python3 .github/scripts/land_dependency_pr.py 6 --read-changelog
land-dependency-pr: git log -1 --pretty=%an <%ae> pull/6/head failed:
  fatal: ambiguous argument 'pull/6/head': unknown revision or path not in the working tree.
```

Both documented invocation styles were affected, for two different reasons:

- `pull/<n>/head` gets **no local ref at all**. GitHub keeps pull-request refs under
  `refs/pull/*`, outside the `+refs/heads/*:refs/remotes/origin/*` refspec every clone has.
- A **branch** name does not resolve either, and not because of the fetch: git reads
  `dependabot/github_actions/x` as remote `dependabot`, branch `github_actions/x`, so the
  shorthand misses the `origin/<branch>` ref the fetch *did* write.

Fixed by `pin_fetched`: resolve the fetch to a commit id once, after the fetch, and use that
everywhere. **This also closes a race the original had** — Dependabot rewrites its branch on
every run, so re-resolving a name later could inspect one commit and merge another. What is
verified is now exactly what is merged. The second `source.ref` use in `preflight`
(`rev-list --count base..source.ref`) was missed by my first attempt at the fix and caught
by the new test.

**Why the reproduction missed it: its fixture asserted a capability GitHub withholds.** A
hand-built bare `origin` can be given whatever refspec the author likes — most naturally one
that *does* publish `refs/pull/*` into the client, which is precisely the world GitHub does
not provide. And the one live PR-number run ticket 39 records was against **PR #4, a human
PR**, which `resolve_pr` refuses on its first check, so `preflight` was never reached on a
real bot pull request. Ticket 39's own Evidence section says the 46 assertions were "against
a faithful reproduction (a real bare `origin`)" — the origin was real, the refspec was not.

Third, smaller finding: a **branch-sourced** landing got `Refs:` pointing at
`tree/<branch>`, a URL Dependabot rewrites on every run and which can 404 after the landing.
A pull-request-sourced landing keeps its PR URL, which is the better link. Only the
branch-sourced case now resolves to the immutable commit URL.

**All three of the ticket's verification questions answered, against real Dependabot data:**

1. **Dependabot's actual commit shape** — matches the fixture closely enough to be
   non-load-bearing, with two details a fixture would plausibly get wrong. Author is
   `dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>` and committer is
   `GitHub <noreply@github.com>`, exempt as committer only. The `Signed-off-by` is
   `dependabot[bot] <support@github.com>` — a **different** address from the author, so a
   check recognising only the author identity would miss it. The body carries an
   `updated-dependencies:` YAML block and the sign-off has **no trailing newline**. The
   script's R1 re-read and the gate both caught it regardless.
2. **`gh pr view`'s fields behave as expected** for a real bot pull request:
   `author.is_bot` is `true`, `author.login` is `app/dependabot`, `isCrossRepository` is
   `false` (Dependabot branches in-repository), `baseRefName` is `main`. `body` is enormous
   (PR #6's is ~15KB of release-notes HTML), which is why `BODY_SCAN_LIMIT` matters — the
   major-bump detection reads the title first and is unaffected.
3. **The gate agrees** on a genuinely re-authored Dependabot commit. See below.

**Evidence, both directions** — the gate asked about the real commits, not a fixture:

```
$ python3 .github/scripts/check_attribution.py --main origin/main..measure/final
check-attribution: OK — 1 commit(s) in 'origin/main..HEAD' clean.        # exit 0

$ python3 .github/scripts/check_attribution.py --main origin/main..<bot sha>
::error::d3a4ab333 R1: trailer 'Signed-off-by: dependabot[bot] <support@github.com>' ...
::error::d3a4ab333 R2: author 'dependabot[bot] <...>' is not the repo owner ...
::error::d3a4ab333 R3: author 'dependabot[bot] <...>' is a bot account ...  # exit 1
```

The bot commit was run through **all five** open pull requests: R1+R2+R3 fire on every one.
`git cherry-pick` was re-measured too and still preserves `dependabot[bot]` as author
(3 violations), confirming ticket 39's claim on live data rather than a fixture.

**The landed commit**, produced from real PR #6 by the fixed script, with the owner's
`git config` identity:

```
author:    Mitun Manav G Y <238927830+mitunmanav@users.noreply.github.com>
committer: Mitun Manav G Y <238927830+mitunmanav@users.noreply.github.com>

ci: bump actions/checkout from 4 to 7

The diff is Dependabot's. The authorship is not: automation proposes, a
person authors, a person merges. See
docs/adr/0009-automated-dependency-landing-path.md.

Dependabot's own `Signed-off-by` is not carried over -- it signed work this
commit did not perform.

Refs: https://github.com/mitunmanav/open-flow/pull/6
```

Seven workflow files changed — the bot's diff, carried whole. No bot sign-off, no bot
author, no bot in the contributor graph.

**On `--read-changelog`, stated plainly.** Every open PR is a major, so the script refused
all five without the flag. That refusal is correct and is not to be worked around. To measure
the *mechanism* I ran the script's own `main()` in memory with `is_major_bump()` stubbed to
`False`, which steps over the owner's accountability claim and **nothing else** — every other
guard, the R1 trailer re-read and `check_attribution.py` all ran. `--read-changelog` is a
claim the owner makes about having read a changelog; it is not mine to assert, and no real
bump was landed. No `--force` or `--skip` flag was added to the script.

**Tests.** New `.github/scripts/test_land_dependency_pr.py`, 10 tests, wired into CI by the
existing `docs-check` glob. The fixture is built to **match GitHub rather than to be
convenient**: `refs/pull/<n>/head` on the bare origin, the default refspec on the client, and
**no** `refs/pull/*` refspec added. Two assertions hold that honest — one fails if
`pull/<n>/head` ever *does* resolve in the fixture, so it cannot drift back into
GitHub-shapedness and stop testing anything; one pins the branch-shorthand trap. Verified
genuinely load-bearing: against ticket 39's original script the suite reports **7 errors**;
against the fixed script **10 pass**; with only the `Refs:` fix reverted, the
branch-sourced-URL test fails. Full suite 59 tests green. `check_docs.py` OK.

Also fixed, incidentally: stdout was block-buffered while refusals go to unbuffered stderr,
so the refusal printed *above* the "Fetching ..." line it belongs under — which made this
very failure actively misleading on first encounter. `sys.stdout.reconfigure(line_buffering=True)`.

**Not done, deliberately.** No PR merged, nothing pushed, `main` untouched. The five majors
need their own outing, and a major landing needs a real changelog read by the owner.
