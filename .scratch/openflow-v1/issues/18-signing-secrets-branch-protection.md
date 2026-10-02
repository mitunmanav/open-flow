# Signing keystore, GitHub secrets, branch protection (human)

Type: task
Status: open
Blocked by: none

## Question

Finish the release-security setup: create the signing keystore locally (never commit), add `OPENFLOW_KEYSTORE_BASE64`, `OPENFLOW_KEYSTORE_PASSWORD`, `OPENFLOW_KEY_ALIAS`, `OPENFLOW_KEY_PASSWORD` as GitHub Secrets, and enable branch protection on `main` (require CI + PR). Confirm `release.yml` can consume them. This blocks the "signed release APK" part of the destination.

## Update (ticket 23)

**The four secret names are confirmed.** `.github/workflows/release.yml` is written and names them, so there is nothing left to derive — set them exactly as listed above. `release.yml` was exercised locally against all three paths: it fails loudly when a secret is missing, rejects a non-semver tag, and passes when all four are present. Ticket 23's acceptance step ("confirm `release.yml` can consume them") is therefore reduced to setting the secrets and tagging.

**Two additions to this ticket:**

1. **Project board.** Ticket 15 settled board columns (`Backlog / Ready / In progress / Verification / Done`) and ticket 23 created all ten labels, but the board itself is unbuilt and needs a scope this session could not grant:
   ```sh
   gh auth refresh -s project
   ```
   Both the `docs` and `attribution` checks report independent statuses, so branch protection can require `docs-check` and `attribution` without depending on the Android build — which is still skipped until `gradlew` exists. That means `ci` / *Lint, test, assemble* is a legitimate required check today and will start doing real work the day the Gradle wrapper lands.

2. **Require the attribution check.** Set `attribution` as a required status check. It enforces the AGENTS.md rule that no bot or AI assistant may appear as an author or co-author — the contributor list is people only. `docs-check` is also worth requiring: it is green today and catches stale documentation the moment it appears.
