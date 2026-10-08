# Commit the landing procedure's assertions

Type: task
Status: open
Blocked by: none

## Task

`land_dependency_pr.py` shipped with its evidence in the authoring session rather
than in the tree: ticket 39's Answer enumerates 46 assertions, all passing, but no
`test_land_dependency_pr.py` exists, so `unittest discover` (which docs-check runs
on every pull request) re-runs none of them. A change to the script has no check
behind it.

Commit the assertions as `.github/scripts/test_land_dependency_pr.py` so the
existing discover pattern picks them up unchanged.

## What the suite must cover

Ticket 39's Evidence section is the list. In short:

- the bot's own commit fails the attribution gate; so does a cherry-pick;
- the script's commit passes, carries the diff, drops the bot's `Signed-off-by`,
  keeps a `Refs:` pointer, and leaves the owner **on** the new branch;
- nothing is pushed and nothing is opened;
- a major bump is refused without `--read-changelog`, a minor one is not;
- each refusal — human branch, dirty tracked file, wrong identity, existing
  branch, local-ahead base, conflicting bump — leaves the tree clean;
- an already-landed bump reports "nothing to land" and makes no empty commit;
- `--dry-run` creates nothing.

## Fixtures the assertions need

A faithful reproduction, not a mock: a real bare `origin` remote, real
bot-authored commits carrying `Signed-off-by`, and the owner identity pinned by
`git config`. Two of the original 46 assertions failed against a *fixture* that
had labelled `8.13 → 9.0` a minor bump — the script was right. Build the fixtures
so the script is the thing under test.

## Out of scope

Changing the script's behaviour, and the `Signed-off-by` left in `TRAILER_RE`
(ticket 39's "Not done" — ADR-0009's rationale is wrong, correctness does not
need it fixed, and editing the regex is a separate decision).
