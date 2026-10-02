# Privacy & data policy

Type: grilling
Status: resolved
Blocked by: 03

## Question

Define V1 data rules: what is stored locally (transcript, provider, timings, status, language, feedback), audio lifetime (delete-by-default, opt-in retention), what context is never collected, permission rationales, and the user-facing privacy page copy for GitHub Pages.

## Answer

Privacy policy settled in `docs/privacy/privacy-policy.md`:

- Local-first: default providers never send audio/transcripts off-device; cloud providers are opt-in, consent-gated, and documented per adapter.
- History stores transcript (+raw), provider, timings, outcome, language, optional feedback; deletable together.
- Audio deleted after successful processing by default; opt-in keeps; sensitive mid-dictation = transcript discarded, never persisted.
- No surrounding-field reading; insertion metadata held only for the insertion attempt.
- No telemetry in V1.
- Settings → Privacy: JSON export, clear-all, retention toggles, cloud consent, sensitive-package list. No default auto-purge.
- Prominent Accessibility disclosure screen before enablement.
