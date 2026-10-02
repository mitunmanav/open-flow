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
- Reference-app teardown of the existing bubble dictation apps, including the
  licence position on each.
- sherpa-onnx and model-selection research for Android: streaming, offline, VAD,
  endpointing, and the V1 default model.
- Privacy policy and Android permissions / Play disclosure checklist.
- Reference prototypes for the bubble, the onboarding flow, and the permission
  state markings.
- Glossary, architecture overview, roadmap, and contributing guide.

### Notes

- Status is pre-alpha: this repository currently contains documentation and
  architecture decisions, not application code. See
  [`ROADMAP.md`](ROADMAP.md).
- A version here will be generated from GitHub release notes by `release.yml`
  on the first `v*` tag, not written by hand.

[Unreleased]: https://github.com/mitunmanav/open-flow/compare/v0.0.0...HEAD