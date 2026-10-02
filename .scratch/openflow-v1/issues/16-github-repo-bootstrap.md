# GitHub repo bootstrap (human)

Type: task
Status: resolved
Blocked by: none

## Question

Create the GitHub remote for open-flow, apply the Apache-2.0 LICENSE, set up the signing keystore plan (secrets strategy — never commit), and create the initial branch protection. Agent cannot do the remote/secrets parts alone; this unblocks release automation decisions.

## Answer

Done:

- `git init -b main`, remote `origin` → https://github.com/mitunmanav/open-flow (public).
- Apache-2.0 `LICENSE` applied; `.gitignore` for Android builds/keystores; root docs skeletons created (README/ROADMAP/CONTRIBUTING/SECURITY/PRIVACY/ARCHITECTURE/THIRD_PARTY_NOTICES/CHANGELOG).
- Initial commit pushed to `main`.

Still on the user (not done, no agent access):

- Create release keystore: `keytool -genkey -v -keystore openflow-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias openflow`
- Add `OPENFLOW_KEYSTORE_BASE64`, `OPENFLOW_KEYSTORE_PASSWORD`, `OPENFLOW_KEY_ALIAS`, `OPENFLOW_KEY_PASSWORD` to GitHub Secrets.
- Enable branch protection on `main` (require PR + required checks) once CI exists.

## Answer

Repo bootstrapped:

- GitHub repo created: **https://github.com/mitunmanav/open-flow** (public), default branch `main`, pushed.
- Apache-2.0 `LICENSE` (copyright: Mitun Manav G Y) added; `.gitignore` excludes keystores, `local.properties`, and local agent tooling.
- Docs skeletons committed: README, ARCHITECTURE, ROADMAP, CONTRIBUTING, SECURITY, PRIVACY, THIRD_PARTY_NOTICES, plus GLOSSARY.md, docs/adr/, docs/architecture/, docs/providers/, docs/privacy/.
- Commit identity pinned to the repo owner; agents never author.
- Remaining human-only step (keystore creation + GitHub secrets + branch protection) graduated to ticket 18.
