# Decide the docs site, and build it

Type: grilling
Status: open
Blocked by: none

## Question

The destination includes a working GitHub Pages site, and ticket 15 settled `pages.yml` (deploy `website/` on push to `main`). But `website/` does not exist, and nothing decided what generates it. `pages.yml` is now written and gated, so it deploys nothing until a site exists — which means the destination's Pages requirement is unsatisfiable until this is answered.

**The decision first: does OpenFlow need a docs site at all?** Right now the docs are 16 Markdown files that render perfectly on GitHub, where the ADRs are readable, diffable, and searchable. That is a real advantage and it is worth stating that a site might only lose things — linkability to source, blame, and `?plain=1` diffs.

Arguments for building it:

- The public audience for provider authors is not reading GitHub ADRs. ADR-0006 deliberately writes provider docs to be *public-facing*, and a raw ADR in a repo of pre-alpha documentation is a poor first impression for someone deciding whether to write an adapter.
- The glossary is the entry point for anyone reading this project, and `GLOSSARY.md` as a flat list of 20 terms does not explain the domain.
- The destination names Pages, so it is a requirement, not a nicety.

If yes, then decide the generator. MkDocs Material is the obvious default for a docs site with no build pipeline of its own and Python already available in CI. The cost is a `mkdocs.yml` plus a theme dependency, and a second place where the doc set is described — which is a staleness risk this repo has already been bitten by twice (ticket 21's unwritten guides, and the empty `CHANGELOG.md` that `docs-check` caught on its first run). Whichever generator is chosen, the CI job that builds the site should fail on a broken internal link, so the site cannot rot independently of the Markdown that feeds it.

If no, close the destination's Pages requirement deliberately rather than leaving `pages.yml` permanently gated, and say so on the map.

Whatever is decided, add the site (and any new config) to `docs/README.md` and keep `docs-check` passing.