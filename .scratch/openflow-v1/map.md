# OpenFlow V1 — Wayfinder Map

## Destination

A locked, executable plan for shipping **OpenFlow V1**: an open-source Android voice-dictation app (floating bubble, not an IME) with a provider-neutral Adaptive Dictation Router, local-first privacy, and a passing three-device / ten-scenario acceptance gate — with CI, release APK, and Pages site working.

## Notes

- Domain: Android voice dictation; NOT an IME; bubble-driven.
- Every ticket should consult `GLOSSARY.md` for vocabulary.
- Standing preferences settled during charting:
  - Name: **OpenFlow**; Apache-2.0; three Gradle modules (`app/`, `core/`, `providers/`) but layout must be easily expandable (documented seams/extension points, no empty modules).
  - Provider-neutral: no `if provider == X` in app code; capabilities-driven.
  - First real provider: sherpa-onnx; FakeProvider for tests; whisper.cpp/cloud providers are future adapters.
  - V1 TranscriptRefiner is deterministic only (no local LLM).
  - History storage: Room once complexity warrants; keep simple at first.
  - Audio deleted after successful processing by default; never retain by default.
  - **Design language: brutalist.** Zero radius, no shadows, flat ground, one accent, hairline rules, 2px structural borders, sentence case, mono for machine-voice copy (set limits in mono against capabilities in the UI face). Accent: electric violet, a separate value per theme, verified WCAG AA for every text role.
  - **Granted/permitted state is shown by the row's own perimeter drawing clockwise** (conic-gradient mask), never a bar bolted to the side of the row.
  - Permissions are requested **contextually, not at first launch** (settled in ticket 03) — so any onboarding flow must justify its order against this, not against a generic wizard.
- Acceptance gate for "shipped": three device classes (Pixel-like, Samsung-class, Xiaomi-class) × ten common text-entry scenarios (short chat, long paragraph, names/jargon, numbers, self-correction, lists, noisy room, weak connection, no connection, multiple languages) all start → record → transcribe → clean → insert → recover cleanly.

## Decisions so far

<!-- one line per closed ticket -->

- Charting-session decisions (pre-ticket): destination, name, module layout, V1 router scope, deterministic refiner, Room-for-history, acceptance gate — all captured in Notes above.
- 01 Reference teardown: bubble→STT→cleanup→insertion consensus documented in `docs/architecture/reference-apps.md`; Sayboard (GPL-3.0) excluded from code reuse; IME-first surfaces, cloud-default cleanup, and APK self-update out of V1.
- 02 sherpa-onnx Android: streaming (OnlineRecognizer) + offline (OfflineRecognizer) + VAD + endpointing documented in `docs/providers/sherpa-onnx.md`
- 03 Permissions & Play policy: checklist + permission-health screen requirements in `docs/privacy/permissions-policy.md`
- 06 Model selection: V1 default = Silero VAD + streaming Zipformer-en-20M int8 (~45 MB); benchmark matrix in `docs/providers/model-selection.md`
- 17 Naming: OpenFlow, dev.openflow.dictation, tagline and original icon brief
- 16 Repo bootstrap: public github.com/mitunmanav/open-flow created and pushed, Apache-2.0 license, docs skeletons, identity pinned
- 15 Repo automation: six workflows (ci/android-test/release/pages/dependency-review/docs-check), templates+labels+board, full doc skeletons, semver tags
- 14 History: Room single transcript_entry table, recoverable errors resurface with re-insert retry, indefinite default retention, destructive migration acceptable pre-1.0
- 13 Module layout: app/core/providers direction, manual wiring, implementation-by-default, documented seams — see `docs/adr/0005-module-layout.md`
- 12 Refiner: deterministic 8-stage pipeline, style formatting last, soft-marker backtracking rule, degradable to raw — see `docs/architecture/refiner.md`
- 11 Router V1: manual preference first, privacy/offline/cost/health rules, capability-matched recoverable fallback, full decision log per dictation — see `docs/adr/0004-adaptive-dictation-router.md`
- 10 V1 feature matrix: full parity list in V1, no demo-thin slice; settings grouped Dictation / Bubble / Dictionary & Snippets / History / Privacy / About
- 09 Privacy: local-first, no telemetry, audio deleted by default, refusal-discards-transcript rule, JSON export/clear-all — see `docs/privacy/privacy-policy.md`
- 08 Insertion: layered SET_TEXT→PASTE→copy+prompt, sensitive-field refusal pre-recording, clipboard preserve/restore, re-resolve by package+characteristics — see `docs/adr/0003-safe-text-insertion.md`
- 07 Bubble UX: Wispr-style interactions (tap/hold/drag/cancel) with fully customizable shape/size/opacity/position in settings; live transcript adjacent bubble, toggleable — see `.scratch/openflow-v1/prototype/bubble.html`
- 05 Dictation state machine: payload-carrying sealed states, single event-channel writer, Inserting non-cancellable, per-step timeouts, refiner failure degrades to raw transcript — see `docs/adr/0002-dictation-state-machine.md`
- 04 SpeechProvider contract: Flow<SpeechEvent> cold flow with conflated Partial, typed FailureReason, capability-driven behavior, one instance per model config, utterance-level timestamps, scriptable FakeProvider — see `docs/adr/0001-speech-provider-contract.md`
- 21 Provider SDK docs: docs-only and public-facing, three files in `docs/providers/`, **no `provider-api` module** and no compatibility promise in V1; capabilities honesty enforced by Contract Tests in core's test fixtures — see `docs/adr/0006-provider-authoring-no-sdk.md`
- 19 Onboarding & permission-health: brutalist permissions page, electric violet, granted state = the row's own perimeter drawing clockwise via conic mask; each ask states what it touches **and** what it cannot do, limit clause set in mono; **flow order deliberately left undecided** — prototype at `.scratch/openflow-v1/prototype/onboarding.html`

## Not yet specified

_(empty — fog cleared; remaining work is ticketed)_
## Out of scope

- Meeting notetaker / always-on transcription product.
- Monetization, accounts, cloud sync.
- iOS / desktop / web clients.
- Automatic ML-based provider routing (V1 router is rules + measurement only).
- Local LLM rewriting in V1.
- Any Wispr code/asset reuse.
