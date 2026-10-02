# Signing keystore, GitHub secrets, branch protection (human)

Type: task
Status: claimed
Blocked by: none

> **Still open, and the rest of it is all human.** Everything an agent can drive is
> done and verified. The signing credentials and the `project` OAuth scope need
> the owner. The checklist is under *Remaining (owner)* below. Not closing the
> ticket until the secrets actually exist, because "signed release APK" is part
> of the destination and an empty secrets list means it cannot ship.

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

## Session record

The blocker turned out to be upstream of everything this ticket asks for: **none of
ticket 23's automation was on the remote.** `main` ran no CI at all, six commits
behind, so there were no check contexts to require and branch protection could not
have meant anything. The gates were only exercised for the first time here, which is
how the two failures below were found at all.

### Done and verified

- **Pushed ticket 23's automation plus this ticket's docs change to `main`.** All
  four push workflows green: `ci`, `docs-check`, `attribution`, `pages`.
- **Branch protection is live on `main`.** Requires a PR, and requires the three
  checks by their real status contexts — `Lint, test, assemble`, `Documentation
  integrity`, `Commit attribution` — resolved by GitHub to Actions `app_id 15368`,
  so they are genuine checks rather than free text that could never report.
  Also: `strict` (branch must be up to date), linear history, force-push blocked,
  deletion blocked, conversation resolution required, admins included
  (`enforce_admins`), and `required_approving_review_count: 0` — this is a
  solo-maintained repo, so requiring one approval would deadlock the owner.
- **Enabled the dependency graph** (`vulnerability-alerts` + `automated-security-fixes`).
  `dependency-review` had been failing on every PR with *"Dependency review is not
  supported on this repository"*. Re-run after enabling: green. This is a fifth
  real defect the gates surfaced, after the four ticket 23 found on first run.
- **Unfiltered `docs-check`** (`b35196b`), which is what made requiring it possible.

### Two findings worth reading before merging anything

1. **`docs-check` could not have been required as written.** Ticket 23 filtered it
   to `**/*.md` plus its own files. Branch protection blocks a PR *forever* when a
   required check never reports, and on a code-only PR that check would never have
   reported. Ticket 23's note that it "is green today" was true only of the runs it
   did have — the filter made the gate decorative. Filter removed; it is a ~2s
   Python script and now runs on every PR.

2. **`attribution` fails every Dependabot PR, by design.** Dependabot PR #1 failed
   with `R3: author 'dependabot[bot]' is a bot account` and a `Signed-off-by:`
   naming the bot. That is the AGENTS.md rule working, and the check says so
   itself: *"Dependabot PRs are welcome; the bot's commits must not be merged into
   main as-is."* Now that `attribution` is **required**, no Dependabot PR can merge
   until a human re-authors it. That is coherent — but ticket 23 enabled Dependabot
   without deciding how its PRs are ever meant to land, and making the check
   required turned that gap into a blocked queue. The policy is its own decision:
   **ticket 29.**

### Remaining (owner)

Both items are refused-to-do-by-agent, not skipped. The signing question was asked
and left unanswered, so no credential was created.

**1. Signing secrets.** If you would rather I generate them, say so and I will run
the block below; otherwise run it yourself. Either way the keystore must be stored
somewhere that is not this working tree.

```sh
# Strong random passwords — never reused, never committed.
STORE_PW=$(openssl rand -base64 24)
KEY_PW=$(openssl rand -base64 24)
ALIAS=openflow-upload

keytool -genkeypair -v -keystore ~/openflow-upload.jks -storetype JKS \
  -keyalg RSA -keysize 4096 -validity 10000 -alias "$ALIAS" \
  -dname "CN=OpenFlow, OU=OpenFlow, O=OpenFlow, L=, ST=, C=" \
  -storepass "$STORE_PW" -keypass "$KEY_PW"

base64 -w0 ~/openflow-upload.jks | gh secret set OPENFLOW_KEYSTORE_BASE64
printf '%s' "$STORE_PW" | gh secret set OPENFLOW_KEYSTORE_PASSWORD
printf '%s' "$ALIAS"  | gh secret set OPENFLOW_KEY_ALIAS
printf '%s' "$KEY_PW" | gh secret set OPENFLOW_KEY_PASSWORD
```

`*.jks` is already gitignored, and `~/` keeps it out of the tree regardless.
Store `~/openflow-upload.jks`, both passwords, and the alias in a password
manager. **A lost keystore means no future release can be signed with the same
key** — so back it up before you need it, not after. Verify with
`gh secret list`, which should then show all four.

**2. Project board.** `gh auth refresh -s project` is an interactive device-code
flow and cannot be scripted; the current token has only
`gist, read:org, repo, workflow`. Then create the board with ticket 15's five
columns (`Backlog / Ready / In progress / Verification / Done`). The ten labels
already exist, so `gh label list` should show them today.

### One thing this ticket cannot finish

`release.yml`'s `verify` job consumes the four secret **names** and was already
exercised in ticket 23. Its `build` job consumes the **keystore**, and the tree has
no Gradle signing config reading `signing.properties` — that is ticket 27. So a
green release run is still unreachable until the Gradle skeleton lands, no matter
how these secrets are set. Ticket 18 can honestly be closed once the four secrets
exist; the signed APK itself waits on 27.
