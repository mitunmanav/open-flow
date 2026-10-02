# AGENTS.md

## Agent skills

### Issue tracker

Issues live as local markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role triage vocabulary. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout (`GLOSSARY.md` + `docs/adr/` at root). See `docs/agents/domain.md`.

## Git and attribution

- **Author identity**: commit only as the repo owner, `Mitun Manav G Y <238927830+mitunmanav@users.noreply.github.com>` (`@mitunmanav`). This is pinned in this repo's local git config. Never invent a placeholder identity, and never use a fake or shared email.
- **Co-author trailers**: any commit produced with agent help carries a `Co-authored-by:` trailer naming the human owner, so the contributor graph attributes the work to `@mitunmanav`. Agents are never the author.
- **No bots in the contributor list**: bots and automation (`github-actions[bot]`, `dependabot`, any `*-bot` account) must never commit to this repository's branches — CI commits artifacts, releases, and Pages deploys through Actions only. Do not add `Co-authored-by:` trailers for bots, and do not use a bot account as a commit author.
- If a past commit has the wrong identity, amend and force-push (owner-authorized) rather than leaving it.
