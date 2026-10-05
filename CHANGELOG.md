# Changelog

All notable changes to OpenFlow are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html) with `v0.x` tags.

OpenFlow has released no version yet. Everything below is unreleased work.

## [Unreleased]

### Added

- Architecture decision records 0001–0006: the SpeechProvider contract, the
  dictation state machine, safe text insertion, the Adaptive Dictation Router,
  the module layout, and provider authoring as docs-only.
- The Gradle project: a committed wrapper on Gradle 8.13 with AGP 8.13.0 and
  Kotlin 2.0.21, `compileSdk`/`targetSdk` 36, `minSdk` 26, the three modules
  ADR-0005 specifies, and the sherpa-onnx dependency resolving from JitPack.
  `./gradlew lint test assembleDebug` passes — but runs no tests yet, because
  there is still no application code. See ADR-0007.
- Reference-app teardown of the existing bubble dictation apps, including the
  licence position on each.
- sherpa-onnx and model-selection research for Android: streaming, offline, VAD,
  endpointing, and the V1 default model.
- Privacy policy and Android permissions / Play disclosure checklist.
- Provider guides promised by ADR-0006: how to implement and register a
  `SpeechProvider`, the Contract Test suite that makes capabilities honesty
  enforceable, and when to propose a provider with its licensing obligations.
- Reference prototypes for the bubble, the onboarding flow, and the permission
  state markings.
- Glossary, architecture overview, roadmap, and contributing guide.
- The project site: four hand-written pages in `website/`, deployed by
  `.github/workflows/pages.yml`. Home, How it works, Privacy, Get involved. No generator and no
  build step — GitHub Pages serves the directory as committed. The hero is the
  app running: a working dictation you can hold, drag and throw, with the
  states of ADR-0002 and the refiner's own stages. Every claim on it is set
  against what it cannot do, and each section names the document it comes from.
- `docs-check` gained two rules for the site: every page must list every page in
  the same order in its nav, and every relative `href`, `src` and CSS `url()`
  must exist on disk. Hand-written pages drift; nothing else would have noticed.
- The site's two typefaces are committed as variable woff2 rather than loaded
  from Google Fonts, because a privacy-first project's landing page must not
  send a visitor's IP to a font CDN.
- The sherpa-onnx research docs described an API that does not exist. Corrected
  against the artifact itself: the Kotlin API ships inside the AAR rather than
  being copied into the consumer, the pinned tag is `v1.13.8` and not `v1.13.5`,
  Silero VAD is 629 KB rather than ~2 MB, `acceptWaveform` takes a sample rate as
  a second argument on both stream classes, and asset-vs-file loading is the
  nullable `AssetManager` constructor argument (`newFromAsset`/`newFromFile` are
  private natives). A reader following the old text wrote code that would not
  compile.
- **The acceptance gate is now enforced.** `.github/scripts/check_gate.py`
  resolves coverage for one signed artifact and refuses to publish when the bar
  the tag's own major version implies is not met, and the `gate` job in
  `.github/workflows/release.yml` runs it before the publish job can start. It
  reads only the fenced `json` block under `## Gate status` in
  `docs/quality/acceptance-gate.md` and the ABI list out of the build, so a
  reworded sentence cannot move a gate. It refuses an unknown `gate_version`, a
  missing or duplicated required-scenario registry, a Device Class key that
  disagrees with its own Hostility Profile, a malformed run list, and any
  artifact identity that does not match the record. A cell on an ABI the build
  does not ship, and a class whose profile holds any `unknown`, are excluded from
  coverage without being judged. `gate_waiver_reason` is non-empty to publish with
  insufficient coverage and is recorded in the release notes; it cannot waive a
  mismatched artifact, an invalid record or an unreviewed protocol. See ADR-0008.
- **Releasing is now a two-step act.** A `v*` tag builds a signed **candidate**
  and nothing else: a 90-day Actions artifact plus a manifest naming its tag,
  source commit, version, SHA-256 and signer, so a tester on a device class the
  project cannot buy has something real to install. Publication is a manual
  promotion dispatched from protected `main` that names the reviewed evidence
  SHA, the candidate run ID and the candidate artifact ID, and promotes those
  exact bytes. Nothing rebuilds during promotion, and a rebuild whose bytes differ
  from a published APK is refused rather than uploaded.
- **A release's version comes from its tag.** `versionName` and `versionCode` are
  derived from `vMAJOR.MINOR.PATCH` rather than hardcoded, so the tag and the
  artifact's own metadata cannot disagree. A release build without a tag now
  fails, including the unsigned build CI uses to measure the APK's size —
  permitting an unsigned artifact and saying which version it is are separate
  permissions.
- `docs-check` gained a rule comparing the gate's required-scenario registry
  against the scenario table IDs exactly, by identity rather than by count, so a
  scenario added to the table without its registry entry fails and a same-sized
  set with a replaced ID fails too. Dated historical counts in prose are left
  alone: they are true statements about the past.
- Tests for both checkers, run on every pull request
  (`python3 -m unittest discover -s .github/scripts -p 'test_*.py'`). They read
  the real protocol, so a schema change the tests have not been taught about
  fails in review rather than in somebody's release.

### Notes

- Status is pre-alpha: this repository currently contains documentation and
  architecture decisions, not application code. See
  [`ROADMAP.md`](ROADMAP.md).
- A version here will be generated from GitHub release notes by
  `.github/workflows/release.yml` on the first published tag, not written by hand.
- **The gate is enforcing and it is red, which is the intended state.** Device
  coverage is `N/3` classes because no run has been performed, so the first
  release needs either the hardware or a written `gate_waiver_reason`. The gate's
  own release has never been exercised end to end, because no tag exists yet; it
  is verified against fixtures and against the real empty record, which is a
  weaker claim than having watched it block and then allow a real tag.

[Unreleased]: https://github.com/mitunmanav/open-flow/compare/v0.0.0...HEAD