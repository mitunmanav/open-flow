# Exercise the landing procedure on a real Dependabot bump

Type: task
Status: blocked:human
Parked on: Await an eligible Dependabot minor/patch PR; owner may trigger a check or await the scheduled run.
Blocked by: none

## Question

Ticket 39 built and verified the landing procedure against a faithful reproduction.
Nobody has run it on a real Dependabot pull request. This is the first outing, and the
point of it is not the bump — it is finding out whether the reproduction was faithful.

## Current prerequisite state

Checked live on 2026-10-04:

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
