# Contributing

Open Flow is MIT. Small patches welcome.

- Android / Kotlin. INTERNET declared; unused until the user picks a net path.
- Dictation is a bubble + Accessibility, not a keyboard.
- Author: **Mitun only.** No Co-Authored-By. No bot authors (Dependabot, github-actions, Copilot).
- Tests: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- Device loop (this laptop): [docs/testing.md](docs/testing.md) — `scripts/qa/gate.sh`
- Do not commit keystores, `local.properties`, `.env`, agent scratch, or personal data.
- Questions: [Discussions](https://github.com/mitunmanav/open-flow/discussions). Bugs: [Issues](https://github.com/mitunmanav/open-flow/issues/new/choose).

## Dev vs Launch

Contributor-only. Not user docs.

- **Dev:** Windows AVD `of_win` + wrap-adb. `./gradlew :app:assembleDebug`, `scripts/qa/gate.sh --quick`.
- **Verify:** `./gradlew :app:testDebugUnitTest :app:lintDebug`. Device loop: [docs/testing.md](docs/testing.md).
- **Launch:** CI + `release.yml` + `pages.yml` ship releases and site. Play readiness: `scripts/qa/play-check.sh`, `docs/store/`.

Author **Mitun only**. No `Co-Authored-By`. Push and tag only on explicit GO.
