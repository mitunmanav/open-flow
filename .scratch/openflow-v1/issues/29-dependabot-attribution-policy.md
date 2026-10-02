# Dependabot PRs vs the required attribution check

Type: grilling
Status: open
Blocked by: none

## Question

Ticket 23 enabled Dependabot; ticket 18 made `attribution` a required check. Those
two are now in direct conflict and no Dependabot PR can ever merge as-is. What is
the intended path for a bot-authored dependency bump to reach `main` without
weakening the rule in AGENTS.md that no bot appears as author or co-author?

## Why this is a decision and not a bug

The attribution check is behaving exactly as specified. On Dependabot PR #1 it
reported:

- `R3: author 'dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>' is a bot account`
- `R3: committer 'GitHub <noreply@github.com>' is a bot account`
- `R1: trailer 'Signed-off-by: dependabot[bot] <support@github.com>' names a bot or an assistant`

and then said, in its own output: *"Dependabot PRs are welcome; the bot's commits
must not be merged into main as-is."*

So the intent is clear and the enforcement is correct. What nobody ever decided is
the **mechanism**: ticket 23 turned the bot on without a landing path, and ticket 18
turning the check required converted that silence into a queue of permanently red
PRs. Nothing is broken; something is undecided.

## The options, and what each costs

- **Re-author by hand, then merge.** Cherry-pick the bump onto an owner-authored
  branch and open a PR from that. Preserves the rule absolutely. Cost: a few
  commands per bump, forever, and it is easy to skip — a stale-dependency queue is
  its own kind of rot.
- **Teach `attribution` an explicit, narrow exemption for bot *authors* on
  unmerged dependency PRs.** Would weaken the rule at exactly the point AGENTS.md
  draws it. The `Signed-off-by` trailer is separate — that one is appended by
  GitHub's merge machinery and arguably should be tolerated even for human PRs,
  since the rule in AGENTS.md names `Co-authored-by`.
- **Drop Dependabot** and check dependencies on a cadence by hand or with a
  scheduled read-only report that opens an issue instead of a PR. Honest about the
  cost, loses the automation.
- **Auto-merge with a bot-only allowlist in the exemption.** Smallest human cost,
  largest rule surface, and interacts badly with `enforce_admins` plus linear
  history.

## What the decision must settle

1. Which mechanism, from the list above or another.
2. Whether a bot *author* on a dependency PR is ever acceptable, versus only a bot
   *trailer* appended by merge machinery — these are two different things and the
   current rule conflates them.
3. Whether `attribution.yml` changes, or whether the answer is purely a workflow
   habit with no code change.
4. What happens to Dependabot PR #1, which is currently open and permanently red.

## Facts a session should know

- The live token lacks the `project` scope but has `repo` and `workflow`, so this
  decision can be implemented and verified in-session once made; only the
  interactive `gh auth refresh` in ticket 18 needs the human.
- `dependency-review` (which runs on Dependabot PRs) was fixed in ticket 18 by
  enabling the dependency graph, so the security signal is available on those PRs
  regardless of how this resolves.
