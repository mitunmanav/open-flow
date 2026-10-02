# Repo automation, release & Pages plan

Type: grilling
Status: resolved
Blocked by: 13

## Question

Specify the `.github` setup: ci.yml (lint/test/assembleDebug), android-test.yml (emulator), release.yml (signed APK/AAB, checksums, notes), pages.yml (website/), dependency-review.yml, docs-check.yml; issue templates, PR template, labels, board columns; README/ROADMAP/CONTRIBUTING/SECURITY/PRIVACY/THIRD_PARTY_NOTICES skeletons.

## Answer

Repo automation settled:

- `.github/workflows/`: ci.yml (lint/test/assembleDebug + debug APK artifact, required before merge), android-test.yml (emulator suite on demand), release.yml (on v* tag: tests, signed APK+AAB, SHA-256, GitHub Release notes), pages.yml (website/ on main push), dependency-review.yml (block high-severity), docs-check.yml (links/ADR/manifest).
- Issue templates: bug, feature, provider proposal, security (via SECURITY.md); PR template. Labels as listed in the ticket.
- Board: Backlog/Ready/In progress/Verification/Done. One issue = one visible deliverable.
- Root docs: README, ARCHITECTURE.md, ROADMAP.md, CONTRIBUTING.md, SECURITY.md, PRIVACY.md, THIRD_PARTY_NOTICES.md — authored when the repo is bootstrapped (ticket 16).
- Semver `v0.x` tags, CHANGELOG via release notes.
