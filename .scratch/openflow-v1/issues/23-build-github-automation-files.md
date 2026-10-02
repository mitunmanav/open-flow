# Build .github workflows, issue templates, and repo automation files

Type: task
Status: open
Blocked by: none

## Question

Ticket 15 settled the contents of `.github/` but nothing was ever written: the repository is a single docs commit with no `.github/` directory. That gap blocks ticket 18, whose acceptance step is "confirm `release.yml` can consume the secrets" — there is no `release.yml` to confirm.

Build from ticket 15's decision: `.github/workflows/{ci,android-test,release,pages,dependency-review,docs-check}.yml`; `.github/ISSUE_TEMPLATE/{bug,feature,provider-proposal,security}.md` (security via `SECURITY.md`); `.github/PULL_REQUEST_TEMPLATE.md`; labels and board columns from ticket 15.

The **provider-proposal template is the one hard requirement** — ticket 21's `docs/providers/provider-proposal.md` cross-links it as the single source of truth for proposal criteria, so the guide is currently a forward reference to a file that does not exist. `release.yml` is the other: it defines the secret names ticket 18 sets (`OPENFLOW_KEYSTORE_BASE64`, `OPENFLOW_KEYSTORE_PASSWORD`, `OPENFLOW_KEY_ALIAS`, `OPENFLOW_KEY_PASSWORD`), so it should land before or with ticket 18.

Note `SECURITY.md`, `THIRD_PARTY_NOTICES.md`, and `pages.yml` (which expects a `website/` directory) are likewise referenced but not yet created.