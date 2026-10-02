# OpenFlow Privacy Policy (V1)

## What leaves the device

V1 default providers run fully on-device (local sherpa-onnx). No audio, transcript, or usage data leaves the device. Future cloud providers must: be explicitly enabled by the user, document their data handling in the provider adapter's doc, and be gated behind the router's privacy rules which treat cloud as consent-required.

## What we store locally

- Final transcript, raw STT text (when available, for retry/correction-rate), provider used, timings, status outcome, language, optional user feedback.
- Both raw and refined text are marked deletable together.
- Sensitive-field dictations are refused before recording; if the focused field becomes sensitive mid-dictation, the transcript is discarded entirely and never persisted.

## Audio

Deleted after successful processing by default. Optional opt-in settings: keep-all-recordings, or keep audio for last N failures (both off by default). Kept audio is viewable and deletable in Settings → Privacy.

## Context

We do not read surrounding field text. Only the metadata needed for insertion is captured at dictation start (package, view id, input type), held for the insertion attempt, and not persisted.

## Telemetry

None in V1: no Firebase, no usage analytics, no crash reporting vendor. Any future anonymous quality telemetry is off by default and clearly disclosed.

## Your controls

Settings → Privacy: Export history (JSON), Clear all history & settings, audio retention toggles, cloud-provider consent, and the list of blocked sensitive packages. History is kept indefinitely by default; optional "auto-delete after N days" is never default-on.

## Accessibility disclosure

Before enabling Accessibility, the app shows a prominent disclosure of exactly what it reads/writes. The Accessibility API is used only for text insertion, never to bypass Android security or surreptitiously collect data.
