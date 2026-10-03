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

### Notes

- Status is pre-alpha: this repository currently contains documentation and
  architecture decisions, not application code. See
  [`ROADMAP.md`](ROADMAP.md).
- A version here will be generated from GitHub release notes by `release.yml`
  on the first `v*` tag, not written by hand.

[Unreleased]: https://github.com/mitunmanav/open-flow/compare/v0.0.0...HEAD