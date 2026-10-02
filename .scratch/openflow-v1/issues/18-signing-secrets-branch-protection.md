# Signing keystore, GitHub secrets, branch protection (human)

Type: task
Status: open
Blocked by: 23

## Question

Finish the release-security setup: create the signing keystore locally (never commit), add `OPENFLOW_KEYSTORE_BASE64`, `OPENFLOW_KEYSTORE_PASSWORD`, `OPENFLOW_KEY_ALIAS`, `OPENFLOW_KEY_PASSWORD` as GitHub Secrets, and enable branch protection on `main` (require CI + PR). Confirm `release.yml` can consume them. This blocks the "signed release APK" part of the destination.
