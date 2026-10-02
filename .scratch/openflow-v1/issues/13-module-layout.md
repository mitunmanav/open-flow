# Expandable module layout

Type: grilling
Status: resolved
Blocked by: 04

## Question

Confirm the Gradle module layout: `app/`, `core/` (audio, dictation, stt, refinement, insertion, routing, model), `providers/` (sherpa first) — with documented seams so new providers/features add modules without restructuring. How do we keep it small but genuinely extensible?

## Answer

Module layout settled (detail in `docs/adr/0005-module-layout.md` and `docs/architecture/modules.md`):

- Three modules only: `app/`, `core/` (audio, dictation, stt, refinement, insertion, routing, model), `providers/sherpa/`.
- Hard direction: `app → core ← providers/*`; core never sees providers; providers see core contracts only.
- New provider = new Gradle module + one registry entry. New feature = new core package + controller wire-in. No empty placeholder modules.
- Manual constructor wiring in `app`; no DI framework in V1.
- `implementation` by default, `api` only for re-exported contract types.
- Tests: core unit tests + FakeProvider pipeline on PR; provider contract tests; app instrumented tests on separate workflow.
