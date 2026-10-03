# Dependabot PRs vs the required attribution check

Type: grilling
Status: resolved
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

## Answer

**Automation proposes; a human authors; a human merges.** The rule binds the landed
commit's attribution, not the diff's provenance — which is the reading `AGENTS.md`'s own
text supports, and the only one under which the rule and Dependabot coexist without a
carve-out. Full reasoning in `docs/adr/0009-automated-dependency-landing-path.md`, which is the
index of this answer rather than a copy of it.

Settled, in the order the questions were asked:

1. **The rule is about attribution.** `AGENTS.md` forbids a bot from being a commit
   author or a `Co-authored-by` trailer, and forbids automation writing to a branch.
   Neither is a claim about where a diff came from. A provenance reading would have made
   "re-author by hand" a violation rather than a solution.
2. **The policy is general, Dependabot as the worked example** — `AGENTS.md` names
   `github-actions[bot]` too, and this repo already has Actions committing releases and
   Pages deploys.
3. **Dependabot stays.** A floor maintained a few times a month is cheap; floors set by
   hand drift silently.
4. **The bot's `Signed-off-by` is stripped on re-author; the trailer stays policed.**
   This one is a correction to what the ticket assumed. The trailer is appended by
   Dependabot to commits it authors itself — verified in the head commit of PR #1 — not by
   GitHub's merge machinery, so the existing `GitHub <noreply@github.com>` committer
   exemption does not reach it and it is not what that carve-out was for.
5. **A non-authorship `Refs:` pointer is required.** GitHub links a squash-merged PR as
   `(#7)` in the subject for free; the manual route loses it, so without an explicit
   pointer the audit trail for every bump vanishes the moment the landing path goes
   manual.
6. **No security-fix carve-out.** `automated-security-fixes` arrives as
   `github-actions[bot]` PRs too, so a critical CVE waits for the owner. Ticket 28's
   `gate_waiver_reason` precedent deliberately does **not** transfer: a skipped acceptance
   gate is recoverable, a bot-authored commit in `main`'s history is not. The latency is
   minutes of commands, not a review cycle.
7. **A script, plus documentation.** `enforce_admins` is on and the repo is
   solo-maintained, so a remotely-red run has no cheap escape hatch while a local one
   costs a minute. This check has already produced one false positive that made the repo
   unmergeable.

### What the investigation changed

Three findings from reading the code and the live repository reshaped the answer:

- **No GitHub-native merge can land a bot PR.** Squash-merge attributes the squash to the
  PR's *author* — which is why PRs #2 and #3 landed as `Mitun Manav G Y` — so a Dependabot
  squash would be bot-authored and fail R3 *and* R2. Rebase-and-merge preserves the bot
  author and also fails. Only a local owner-authored commit works, which makes the
  re-author route forced rather than preferred.
- **`git cherry-pick` preserves the original author**, so the obvious implementation of
  "re-author by hand" produces exactly the commit the check exists to reject. It also
  copies the message verbatim, dragging the sign-off line along.
- **`main` needs no history repair.** Ten commits, all owner-authored; the two web-UI
  squashes carry the owner's name and pass R2 on name match alone. The three-line failure
  quote in this ticket's Context predates the `a25045f` committer exemption and is not
  current output.

### Consequences worth carrying

- A bot-authored commit can still be pushed to an open branch; it simply will not merge.
  The check is a merge gate, not a branch guard.
- `Signed-off-by` remains policed **even though its R1 rationale is wrong** — "GitHub
  counts trailer names as contributors" is true of `Co-authored-by` and false of a DCO
  sign-off. Correctness does not need the rationale fixed here, so it was not bundled in.

### What this decision sent back out

Two follow-ups, both execution rather than decision:

- **`.scratch/openflow-v1/issues/38-narrow-dependabot-groups.md`** — split major bumps
  from routine ones in `.github/dependabot.yml`, and close PR #1. Neither is blocked: it
  is simply cheaper, and it reduces the blast radius of whatever lands first.
- **`.scratch/openflow-v1/issues/39-dependabot-landing-procedure.md`** — the script, the
  documented procedure, and the `CONTRIBUTING.md` rule.

Nothing graduated from the map's fog. The bubble, the recovery surface, and onboarding
order are untouched by this decision.
