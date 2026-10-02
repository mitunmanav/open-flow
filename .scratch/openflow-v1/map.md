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
  - **CI is green from the first commit.** Workflows whose steps need files that do not exist yet probe for them and skip rather than failing, so a required check is usable while the code is still being written. Settled in ticket 23; this is the standing rule for any workflow added later.
  - **No stale documents.** A document that points at a file that does not exist, or promises one nobody wrote, is a bug — a reader cannot tell it from a correct one. `docs-check` enforces it; see ticket 23.
  - **No bots in the contributor list — enforced, not just intended** (settled in ticket 23, which put `attribution.yml` behind the rule AGENTS.md already stated). No `Co-authored-by` or `Signed-off-by` trailer may name a bot, an automation account, or an AI assistant; `main` is owner-authored only; automation never writes to a branch here. Human co-authors are fine, including the trailers GitHub appends by itself when squashing.
  - **`main` is branch-protected, and the gates only started working once ticket 23 was actually pushed** — until then `main` ran no CI whatsoever, so nothing below had been exercised. Live as of ticket 18: a PR is required, and `Lint, test, assemble`, `Documentation integrity`, and `Commit attribution` are required contexts (resolved by GitHub to Actions `app_id 15368`, so they are real checks, not free text). Linear history, no force-push, no deletion, conversation resolution required, admins included. **`required_approving_review_count` is deliberately 0** — this is a solo-maintained repo and one approval would deadlock the owner. Because `enforce_admins` is on, *nothing reaches `main` except a green PR, including the owner's own pushes.*
  - **A required status check must be able to report on every PR, and must be able to pass.** A `paths:` filter on a check that branch protection requires is a deadlock: GitHub blocks the PR forever waiting for a check that will never run. This is why `docs-check` is deliberately **unfiltered** — as originally written it only ran on `**/*.md` changes, so on a code-only PR it never reported and the gate was decorative. Any check added to the required list must run unconditionally on `pull_request`, and must be verified end-to-end on a real PR before it is required — verify the trigger *and* the outcome, not the trigger alone. Requiring a check nobody has ever seen pass is worse than not requiring it: it blocks every merge while looking configured.
  - **`GitHub <noreply@github.com>` is GitHub's merge machinery, not an automation account**, and must never be treated as a bot. It is the committer of every synthetic `pull_request` test-merge commit and of any web-UI squash. `check_attribution.py` now exempts it as committer only — exempting it as author, or exempting `[bot]` accounts, would gut the rule AGENTS.md states. See ticket 18, where requiring `attribution` without this made the repo unmergeable.
  - **A web-UI squash re-attributes the author email** to the account's public address (`mitunmanav933@gmail.com`), not the identity pinned in AGENTS.md. `attribution` still passes because `is_owner` matches on name, so R2 is one display-name change away from breaking. It is a GitHub account setting, not a repo one.
  - **The dependency graph is enabled** (`vulnerability-alerts` + `automated-security-fixes`), so `dependency-review` actually runs instead of failing with "not supported on this repository".
  - **Dependabot's landing path is undecided — ticket 29.** `attribution` is required and a bot *author* fails it, which is AGENTS.md working as written, but ticket 23 enabled Dependabot without deciding how its PRs are meant to merge. Do not assume a red Dependabot PR is a malfunction.
  - **`release.yml`'s `verify` job and `build` job are at different maturities.** `verify` only reads the four secret *names* (exercised in ticket 23); `build` needs a Gradle signing config reading `signing.properties` that does not exist in the tree yet — ticket 27. A green release run is unreachable until 27 lands, independent of whether the signing secrets are set.
- Acceptance gate for "shipped": three device classes (Pixel-like, Samsung-class, Xiaomi-class) × ten common text-entry scenarios (short chat, long paragraph, names/jargon, numbers, self-correction, lists, noisy room, weak connection, no connection, multiple languages) all start → record → transcribe → clean → insert → recover cleanly. **The protocol for running and recording that gate is still undecided — ticket 28.**

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
- 22 ProviderHealth & cost shape: two vocabularies not one — `ProviderHealth` (`HEALTHY/DEGRADED/UNAVAILABLE/MODEL_MISSING`) separate from `ProviderState`, `health()` a plain cached-truth method; `DEGRADED` ranks last rather than sinking; router rules now ordered and empty sets attributed to a rule; `estimatedCostAvailability` deleted for `pricing: Pricing?` (micros-USD/sec, `0` = free, `null` = cannot estimate) with the ceiling compared against a worst-case bound — see `docs/adr/0001-speech-provider-contract.md` and `docs/adr/0004-adaptive-dictation-router.md`
- 23 Repo automation files: seven workflows (the six from ticket 15 plus `attribution.yml`), four issue templates, PR template, CODEOWNERS, dependabot, ten labels live on the repo; `release.yml` names the four signing secrets ticket 18 sets; **stale-document gate is a ratchet, not an exemption list** (`.github/scripts/check_docs.py` + `.github/docs-baseline.txt`); attribution rules now CI-enforced. Found four real defects on first run, including ticket 21's unwritten guides (now ticket 24) and the never-run model benchmark (now ticket 25). Board still needs `gh auth refresh -s project` (folded into ticket 18). **Not pushed** — left for the owner.

## Not yet specified

Fog, in scope but not yet sharp enough to ticket. Expect these to graduate as
the frontier advances.

- **How the bubble is actually built.** Ticket 07 settled shape, size, opacity,
  position, and the tap/hold/drag/cancel interactions; ticket 20 is claiming the
  motion. Nothing has been said about the overlay window's technical shape —
  `TYPE_APPLICATION_OVERLAY` bounds, whether it survives rotation and keyboard
  resize, and how it avoids being captured in screenshots. Likely becomes
  several tickets once the Android shell exists.
- **What "recovery" looks like on screen.** Ticket 14 settled that recoverable
  errors resurface in History with a re-insert retry, and ticket 05 settled that
  the refiner degrades to raw rather than failing. The UI for either — what the
  user sees, and what they can do about it — is untouched.
- **The dictionary, snippets, and styles feature surface.** Ticket 10 put full
  parity in V1 and ticket 12 settled the refiner stage that consumes a dictionary,
  but what a user actually types into a dictionary entry, and whether snippets
  are per-app, is unspecified. Sharpens once ticket 27 lands.
- **Onboarding flow order.** Deliberately left undecided in ticket 19 pending
  the contextual-permission decision in Notes. This is the one place the map has
  recorded leaving something open on purpose, and ticket 19's prototype is the
  asset to react to.

## Out of scope

- Meeting notetaker / always-on transcription product.
- Monetization, accounts, cloud sync.
- iOS / desktop / web clients.
- Automatic ML-based provider routing (V1 router is rules + measurement only).
- Automatic health-based rerouting mid-dictation (V1 reads a health snapshot at `PREPARING`).
- A user-facing cost ceiling control (no billable provider in V1; the field exists so the first cloud adapter is a drop-in).
- Multi-currency cost conversion (micros-USD only; a non-USD provider converts at its own boundary or reports unknown).
- Monthly spend budgets (V1's ceiling is per-dictation; a budget needs metering and a period).
- Local LLM rewriting in V1.
- Any Wispr code/asset reuse.
